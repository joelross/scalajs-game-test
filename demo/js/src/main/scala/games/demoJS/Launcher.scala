package games.demoJS

import scala.scalajs.js
import org.scalajs.dom
import scalajs.concurrent.JSExecutionContext.Implicits.queue

import games._
import games.math
import games.math.Vector3f
import games.opengl._
import games.audio._

import games.demo._

object Launcher extends js.JSApp {
  def main(): Unit = {
    JsUtils.setResourcePath("/resources")

    val output = dom.document.getElementById("demo-output")
    val canvas = dom.document.getElementById("demo-canvas-main").asInstanceOf[dom.html.Canvas]

    val itf = new EngineInterface {
      def printLine(msg: String): Unit = {
        val line = dom.document.createElement("p")
        line.innerHTML = msg
        output.appendChild(line)
      }
      def getScreenDim(): (Int, Int) = {
        val width = canvas.width
        val height = canvas.height
        (width, height)
      }
      def init(): (GLES2, Context) = {
        val classicWebGLContext = canvas.getContext("webgl").asInstanceOf[js.UndefOr[dom.raw.WebGLRenderingContext]]
        val experimentalWebGLContext = canvas.getContext("experimental-webgl").asInstanceOf[js.UndefOr[dom.raw.WebGLRenderingContext]]
        val webGL: dom.raw.WebGLRenderingContext = classicWebGLContext.orElse(experimentalWebGLContext).getOrElse(throw new RuntimeException("WebGL not supported by the browser"))
        js.Dynamic.global.WebGLDebugUtils.init(webGL)
        val glContext: GLES2 = new GLES2WebGL(webGL)
        val audioContext: Context = new WebAudioContext
        (glContext, audioContext)
      }
      def update(): Boolean = true
      def close(): Unit = {}
    }

    val engine = new Engine(itf)

    Utils.startFrameListener(engine)
  }
}
