package games.demoJVM

import java.io.FileInputStream
import java.io.File
import java.io.EOFException

import org.lwjgl.opengl._

import games._
import games.math
import games.math.Vector3f
import games.opengl._
import games.audio._
import games.input._

import games.demo._

import scala.concurrent.ExecutionContext.Implicits.global

object Launcher {
  def main(args: Array[String]): Unit = {
    val manager = games.JvmUtils.initGLFWManager()
  
    val itf = new EngineInterface {
      def initGL(): GLES2 = new GLES2LWJGL()
      def initAudio(): Context = new ALContext()
      def initKeyboard(): Keyboard = new KeyboardLWJGL()
      def initMouse(): Mouse = new MouseLWJGL()
      def initTouch(): Option[Touchscreen] = None
      def initAccelerometer: Option[Accelerometer] = None
      def continue(): Boolean = ??? //!Display.isCloseRequested()
    }

    val engine = new Engine(itf)

    Utils.startFrameListener(engine)
    
    manager.loop()
  }
}
