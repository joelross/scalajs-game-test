package games.demoJS

import scala.scalajs.js
import org.scalajs.dom
import games.demo.Data
import transport.javascript.WebSocketClient
import transport.WebSocketUrl

import scalajs.concurrent.JSExecutionContext.Implicits.queue

object Launcher extends js.JSApp {
  def main(): Unit = {
    this.println("JS " + Data.text)
    
    this.println("Connecting to " + Data.server)

    val futureConnection = new WebSocketClient().connect(WebSocketUrl(Data.server))
    futureConnection.foreach { connection =>
      connection.write("Hello from JS client")
      connection.handlerPromise.success { m =>
        this.println("Message received from server: " + m)
      }
    }
  }

  val output = dom.document.getElementById("demo-output")

  def println(msg: String): Unit = {
    val line = dom.document.createElement("p")
    line.innerHTML = msg
    output.appendChild(line)
  }
}
