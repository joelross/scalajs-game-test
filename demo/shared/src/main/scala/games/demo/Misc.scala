package games.demo

import scala.concurrent.{ Future, ExecutionContext }
import games.{ Utils, Resource }
import games.math._

import scala.collection.immutable

object Misc {
  def loadConfigFile(resourceConfig: Resource)(implicit ec: ExecutionContext): Future[immutable.Map[String, String]] = {
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

  def conv(v: network.Vector3): Vector3f = new Vector3f(v.x, v.y, v.z)
  def conv(v: Vector3f): network.Vector3 = network.Vector3(v.x, v.y, v.z)
  def conv(v: network.Vector2): Vector2f = new Vector2f(v.x, v.y)
  def conv(v: Vector2f): network.Vector2 = network.Vector2(v.x, v.y)
  def conv(v: network.State): State = v match {
    case network.Absent => Absent
    case network.Present(uPosition, uVelocity, uOrientation, uHealth) => new Present(conv(uPosition), conv(uVelocity), uOrientation, uHealth)
  }
  def conv(v: State): network.State = v match {
    case Absent     => network.Absent
    case x: Present => network.Present(conv(x.position), conv(x.velocity), x.orientation, x.health)
  }
}
