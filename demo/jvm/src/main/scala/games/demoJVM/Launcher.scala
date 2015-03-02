package games.demoJVM

import scala.concurrent.ExecutionContext.Implicits.global
import games.demo.Engine
import java.io.FileInputStream
import java.io.File
import games.audio.VorbisDecoder
import java.io.EOFException
import games.audio._

object Launcher {

  def main(args: Array[String]): Unit = {
    def printLine(m: String): Unit = {
      println(m)
    }

    //    val engine = new Engine(printLine)
    //    engine.start()

    val in = new FileInputStream(new File("/home/joel/project-git/scalajs-games/demo/shared/src/main/resources/games/demo/test.ogg"))

    val decoder = new VorbisDecoder(in, new FixedUnsigned8Converter)

    println("channels: " + decoder.channels)
    println("rate: " + decoder.rate)

    var count = 0
    try {
      while (true) {
        decoder.getNextPacket()
        count += 1
      }
    } catch {
      case eof: EOFException =>
      case e: Exception      => e.printStackTrace()
    }

    println("Retrieved " + count + " packets")

    //    println("Press enter to exit")
    //    System.in.read()
    //    println("Client closing...")
  }
}
