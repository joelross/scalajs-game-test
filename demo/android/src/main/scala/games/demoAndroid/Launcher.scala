package games.demoAndroid

import android.os.Bundle
import android.app.Activity
import android.widget.TextView

import java.lang.Runnable

import games.demo.Data
import transport.tyrus.WebSocketClient
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import transport.WebSocketUrl

class Launcher extends Activity {
  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    val tv = new TextView(this)
    setContentView(tv)
    var text: List[String] = Nil
    def printTextViewLine(s: String) {
        runOnUiThread(new Runnable {
            @Override def run(): Unit = {
                text = s :: text
                tv.setText(text.reverse.mkString("\n"))
            }
        })
    }
    
    printTextViewLine("Android " + Data.text)
    
    val jni = new HelloJni
    printTextViewLine("Test jni: " + jni.stringFromJNI())
    
    Future { // Android does not like IO on the UI thread
      printTextViewLine("Connecting to " + Data.server)
      
      val futureConnection = new WebSocketClient().connect(WebSocketUrl(Data.server))
      futureConnection.foreach { connection =>
        connection.write("Hello from Android client")
        connection.handlerPromise.success { m =>
          printTextViewLine("Message received from server: " + m)
        }
      }
    }
  }
}
