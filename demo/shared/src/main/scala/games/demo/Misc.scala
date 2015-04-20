package games.demo

import scala.concurrent.{ Future, ExecutionContext }
import games.{ Utils, Resource }

object Misc {
  def loadConfigFile(resourceConfig: Resource)(implicit ec: ExecutionContext): Future[Map[String, String]] = {
    val configFileFuture = Utils.getTextDataFromResource(resourceConfig)

    for (configFile <- configFileFuture) yield {
      val lines = Utils.lines(configFile)
      lines.map { line =>
        val tokens = line.split("=", 2)
        if (tokens.size != 2) throw new RuntimeException("Config file malformed: \"" + line + "\"")
        val key = tokens(0)
        val value = tokens(1)

        (key, value)
      }.toMap
    }
  }
}