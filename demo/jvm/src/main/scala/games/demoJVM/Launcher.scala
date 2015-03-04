package games.demoJVM

import scala.concurrent.ExecutionContext.Implicits.global
import games.demo.Engine
import java.io.FileInputStream
import java.io.File
import games.audio.VorbisDecoder
import java.io.EOFException
import games.audio._
import games.Resource

object Launcher {

  def main(args: Array[String]): Unit = {
    def printLine(m: String): Unit = {
      println(m)
    }

    val audioContext: Context = new ALContext

    val testResource = new Resource("/games/demo/test.ogg")

    val testData = audioContext.createBufferedData(testResource)
    val s = testData.createSource
    s.onSuccess {
      case s =>
        printLine("Resource ready")
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
