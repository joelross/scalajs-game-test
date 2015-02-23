package games.demoAndroid

import android.os.Bundle
import android.app.Activity
import android.widget.TextView

import java.lang.Runnable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import games.demo.Engine

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
    
    val jni = new HelloJni
    printTextViewLine("Test jni: " + jni.stringFromJNI())
    
    val prejni = new PrecompiledJni
    printTextViewLine("Test precompiled jni: " + prejni.precompiledStringFromJNI())
    
    Future { // Android does not like IO on the UI thread
      val engine = new Engine(printTextViewLine)
      engine.start()
    }
  }
}
