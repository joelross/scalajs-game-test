package games.demo

import scala.concurrent.{ Future, ExecutionContext }
import games.{ Utils, Resource }
import games.math._
import games.input.{ Key, Keyboard }

import scala.collection.immutable

object Misc {
  def loadConfigFile(resourceConfig: Resource)(implicit ec: ExecutionContext): Future[immutable.Map[String, String]] = {
    val configFileFuture = Utils.getTextDataFromResource(resourceConfig)

    for (configFile <- configFileFuture) yield {
      val lines = configFile.lines
      lines.map { line =>
        val tokens = line.split("=", 2)
        if (tokens.size != 2) throw new RuntimeException("Config file malformed: \"" + line + "\"")
        val key = tokens(0)
        val value = tokens(1)

        (key, value)
      }.toMap
    }
  }

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

sealed trait KeyLayout {
  val forward: Key
  val backward: Key
  val left: Key
  val right: Key

  val mouseLock: Key
  val fullscreen: Key
  val renderingMode: Key
  val escape: Key
  val changeLayout: Key

  val volumeIncrease: Key
  val volumeDecrease: Key
}

object Qwerty extends KeyLayout {
  final val forward: Key = Key.W
  final val backward: Key = Key.S
  final val left: Key = Key.A
  final val right: Key = Key.D

  final val mouseLock: Key = Key.L
  final val fullscreen: Key = Key.F
  final val renderingMode: Key = Key.M
  final val escape: Key = Key.Escape
  final val changeLayout: Key = Key.Tab

  final val volumeIncrease: Key = Key.NumAdd
  final val volumeDecrease: Key = Key.NumSubstract
}

object Azerty extends KeyLayout {
  final val forward: Key = Key.Z
  final val backward: Key = Key.S
  final val left: Key = Key.Q
  final val right: Key = Key.D

  final val mouseLock: Key = Key.L
  final val fullscreen: Key = Key.F
  final val renderingMode: Key = Key.M
  final val escape: Key = Key.Escape
  final val changeLayout: Key = Key.Tab

  final val volumeIncrease: Key = Key.NumAdd
  final val volumeDecrease: Key = Key.NumSubstract
}
