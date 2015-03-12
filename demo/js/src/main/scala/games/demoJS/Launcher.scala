package games.demoJS

import scala.scalajs.js
import org.scalajs.dom
import scalajs.concurrent.JSExecutionContext.Implicits.queue
import games.demo.Engine
import games.audio.JsContext
import games.Resource
import games.JsUtils
import games.audio.Context
import games.math.Vector3f

object Launcher extends js.JSApp {
  def main(): Unit = {
    JsUtils.setResourcePath("/resources")

    val output = dom.document.getElementById("demo-output")
    def printLine(msg: String): Unit = {
      val line = dom.document.createElement("p")
      line.innerHTML = msg
      output.appendChild(line)
    }

    printLine("Starting demo")

    /*val engine = new Engine(printLine)
    engine.start()*/

    val audioContext: Context = new JsContext
    printLine("Listener is at " + audioContext.listener.position + " and looking at " + audioContext.listener.orientation + " (up is at " + audioContext.listener.up + ")")

    val testResource = new Resource("/games/demo/test_stereo.ogg")

    val testData = audioContext.createStreamingData(testResource)
    val s = testData.createSource
    s.onSuccess {
      case s =>
        printLine("Resource ready")
        s.play
    }
    s.onFailure {
      case t =>
        printLine("Error: " + t.getMessage)
    }
  }

}
