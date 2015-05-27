package games.demoJS

import scala.scalajs.js
import org.scalajs.dom

import games._
import games.math
import games.math.Vector3f
import games.opengl._
import games.audio._
import games.input._

import games.demo._

import scalajs.concurrent.JSExecutionContext.Implicits.queue

object Launcher extends js.JSApp {
  def main(): Unit = {
    JsUtils.setResourcePath("/resources")
    JsUtils.orientationLockOnFullscreen = false
    if (WebAudioContext.canUseAurora) Console.println("Aurora.js available as fallback")

    val output = dom.document.getElementById("demo-output")
    val canvas = dom.document.getElementById("demo-canvas-main").asInstanceOf[dom.html.Canvas]

    val itf = new EngineInterface {
      def printLine(msg: String): Unit = {
        //        val line = dom.document.createElement("p")
        //        line.innerHTML = msg
        //        output.appendChild(line)
        println(msg)
      }
      def initGL(): GLES2 = {
        val glContext: GLES2 = new GLES2WebGL(canvas)
        glContext
      }
      def initAudio(): Context = {
        val audioContext: Context = new WebAudioContext()
        audioContext
      }
      def initKeyboard(): Keyboard = {
        val keyboard = new KeyboardJS()
        keyboard
      }
      def initMouse(): Mouse = {
        val mouse = new MouseJS(dom.document)
        mouse
      }
      def initTouch(): Option[Touchpad] = {
        val touch = new TouchpadJS(dom.document)
        Some(touch)
      }
      def initAccelerometer: Option[Accelerometer] = {
        val acc = new AccelerometerJS()
        Some(acc)
      }
      def update(): Boolean = true
      def close(): Unit = {}
    }

    val engine = new Engine(itf)

    Utils.startFrameListener(engine)
  }
}
