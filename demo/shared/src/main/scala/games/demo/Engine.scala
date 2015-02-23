package games.demo

import transport.WebSocketUrl
import scala.concurrent.ExecutionContext

class Engine(printLine: (String) => Unit)(implicit ec: ExecutionContext) {
  def start(): Unit = {
    printLine(Specifics.platformName + " " + Data.text)
    
    printLine("Connecting to " + Data.server)

    val futureConnection = new Specifics.WebSocketClient().connect(WebSocketUrl(Data.server))
    futureConnection.foreach { connection =>
      connection.write("Hello from " + Specifics.platformName + " client")
      connection.handlerPromise.success { m =>
        printLine("Message received from server: " + m)
      }
    }
  }
}
