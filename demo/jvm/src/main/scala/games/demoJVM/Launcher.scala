package games.demoJVM

import scala.concurrent.ExecutionContext.Implicits.global
import games.demo.Engine

object Launcher {

  def main(args: Array[String]): Unit = {
    def printLine(m: String): Unit = {
      println(m)
    }
    
    val engine = new Engine(printLine)
    engine.start()

    Thread.sleep(5000)
    println("Client closing...")
  }
}
