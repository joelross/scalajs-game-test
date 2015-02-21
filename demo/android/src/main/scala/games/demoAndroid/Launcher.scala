package games.demoAndroid

import android.os.Bundle
import android.app.Activity

import games.demo.Data
import transport.tyrus.WebSocketClient
import scala.concurrent.ExecutionContext.Implicits.global
import transport.WebSocketUrl

class Launcher extends Activity {
  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    println("Android " + Data.text)
    
    println("Connecting to " + Data.server)
    
    val futureConnection = new WebSocketClient().connect(WebSocketUrl(Data.server))
    futureConnection.foreach { connection =>
      connection.write("Hello from Android client")
      connection.handlerPromise.success { m =>
        println("Message received from server: " + m)
      }
    }
  }
}
