package games.demoJVM

import scala.concurrent.ExecutionContext.Implicits.global
import games.demo.Engine
import java.io.FileInputStream
import java.io.File
import games.audio.VorbisDecoder

object Launcher {

  def main(args: Array[String]): Unit = {
    def printLine(m: String): Unit = {
      println(m)
    }

    //    val engine = new Engine(printLine)
    //    engine.start()

    val in = new FileInputStream(new File("/home/joel/project-git/scalajs-games/demo/shared/src/main/resources/games/demo/test.ogg"))

    val decoder = new VorbisDecoder(in)
    
    println("version: " + decoder.info.version)
    println("channels: " + decoder.info.channels)
    println("rate: " + decoder.info.rate)

    var count = 0
    try {
      while (true) {
        decoder.getNextPacket()
        count += 1
      }
    } catch {
      case t => t.printStackTrace()
    }
    
    println("Retrieved " + count + " packets")

    println("Press enter to exit")
    System.in.read()
    println("Client closing...")
  }
}
