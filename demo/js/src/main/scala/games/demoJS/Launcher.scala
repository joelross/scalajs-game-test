package games.demoJS

import scala.scalajs.js
import org.scalajs.dom
import games.demo.Stub

object Launcher extends js.JSApp {
  def main(): Unit = {
    this.println("JS " + Stub.text)
  }

  val output = dom.document.getElementById("demo-output")

  def println(msg: String): Unit = {
    val line = dom.document.createElement("p")
    output.innerHTML = msg
    output.appendChild(output)
  }
}
