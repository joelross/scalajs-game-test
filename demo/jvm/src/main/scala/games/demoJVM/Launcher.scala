package games.demoJVM

import scala.concurrent.ExecutionContext.Implicits.global
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

object Launcher {

  def main(args: Array[String]): Unit = {
    val glMajor = 3
    val glMinor = 0
    val width = 640
    val height = 360
    val title = "Scala.js-games"

    val itf = new EngineInterface {
      def printLine(m: String): Unit = {
        println(m)
      }
      def initGL(): GLES2 = {
        val glContext: GLES2 = new GLES2LWJGL()
        glContext
      }
      def initAudio(): Context = {
        val audioContext: Context = new ALContext()
        audioContext
      }
      def initKeyboard(): Keyboard = {
        val keyboard = new KeyboardLWJGL()
        keyboard
      }
      def initMouse(): Mouse = {
        val mouse = new MouseLWJGL()
        mouse
      }
      def update(): Boolean = {
        Display.update()
        !Display.isCloseRequested()
      }
      def close(): Unit = {
        Display.destroy()
      }
    }

    val engine = new Engine(itf)(JvmUtils.openglExecutionContext)

    Utils.startFrameListener(engine)
  }
}
