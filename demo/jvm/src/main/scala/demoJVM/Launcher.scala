package demoJVM

import demo.Stub

object Launcher {

  def main(args: Array[String]): Unit = {
    println("JVM " + Stub.text)
  }

}