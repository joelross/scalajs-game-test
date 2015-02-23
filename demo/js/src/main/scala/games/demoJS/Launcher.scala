package games.demoJS

import scala.scalajs.js
import org.scalajs.dom

import scalajs.concurrent.JSExecutionContext.Implicits.queue
import games.demo.Engine

object Launcher extends js.JSApp {
  def main(): Unit = {
    val output = dom.document.getElementById("demo-output")
    def printLine(msg: String): Unit = {
      val line = dom.document.createElement("p")
      line.innerHTML = msg
      output.appendChild(line)
    }

    val engine = new Engine(printLine)
    engine.start()
  }

}
