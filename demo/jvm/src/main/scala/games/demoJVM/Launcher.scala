package games.demoJVM

import games.demo.Data
import transport.tyrus.WebSocketClient
import scala.concurrent.ExecutionContext.Implicits.global
import transport.WebSocketUrl

object Launcher {

  def main(args: Array[String]): Unit = {
    println("JVM " + Data.text)
    
    println("Connecting to " + Data.server)

    val futureConnection = new WebSocketClient().connect(WebSocketUrl(Data.server))
    futureConnection.foreach { connection =>
      connection.write("Hello from JVM client")
      connection.handlerPromise.success { m =>
        println("Message received from server: " + m)
      }
    }

    Thread.sleep(5000)
    println("Client closing...")
  }
}
