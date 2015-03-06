package games.demoJVM

import scala.concurrent.ExecutionContext.Implicits.global
import games.demo.Engine
import java.io.FileInputStream
import java.io.File
import games.audio.VorbisDecoder
import java.io.EOFException
import games.audio._
import games.Resource
import games.math.Vector3f

object Launcher {

  def main(args: Array[String]): Unit = {
    def printLine(m: String): Unit = {
      println(m)
    }

    val audioContext: Context = new ALContext
    printLine("Listener is at " + audioContext.listener.position + " and looking at " + audioContext.listener.orientation + " (up is at " + audioContext.listener.up + ")")

    val testResource = new Resource("/games/demo/test_mono.ogg")

    val testData = audioContext.createStreamingData(testResource)
    val s = testData.createSource3D
    s.onSuccess {
      case s =>
        printLine("Resource ready")
        s.position = new Vector3f(-10, 0, 0)
        s.play
    }
    s.onFailure {
      case t => printLine("Error: " + t.getMessage)
    }

    println("Press enter to exit")
    System.in.read()
    println("Client closing...")
  }
}
