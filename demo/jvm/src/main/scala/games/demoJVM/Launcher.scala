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
    val mainThread = Thread.currentThread()
    
    val manager = games.JvmUtils.initGLFWManager(mainThread)
  
    val itf = new EngineInterface {
      lazy val initGL: GLES2 = new GLES2LWJGL()
      lazy val initAudio: Context = new ALContext()
      lazy val initKeyboard: Keyboard = new KeyboardLWJGL(initGL.display)
      lazy val initMouse: Mouse = new MouseLWJGL()
      lazy val initTouch: Option[Touchscreen] = None
      lazy val initAccelerometer: Option[Accelerometer] = None
      def continue(): Boolean = ??? //!Display.isCloseRequested()
    }

    val engine = new Engine(itf)

    Utils.startFrameListener(engine)
    
    manager.loop()
  }
}
