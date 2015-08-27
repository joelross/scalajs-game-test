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

    val canvas = dom.document.getElementById("demo-canvas-main").asInstanceOf[dom.html.Canvas]

    val itf = new EngineInterface {
      def initGL(): GLES2 = new GLES2WebGL(canvas)
      def initAudio(): Context = new WebAudioContext()
      def initKeyboard(): Keyboard = new KeyboardJS()
      def initMouse(): Mouse = new MouseJS(canvas)
      def initTouch(): Option[Touchscreen] = Some(new TouchscreenJS(canvas))
      def initAccelerometer: Option[Accelerometer] = Some(new AccelerometerJS())
      def continue(): Boolean = true
    }

    val engine = new Engine(itf)

    Utils.startFrameListener(engine)
  }
}
