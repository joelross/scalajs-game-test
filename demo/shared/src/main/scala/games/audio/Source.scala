package games.audio

import java.io.Closeable
import games.math.Vector3f

abstract class AbstractSource extends Closeable {
  def play: Unit
  def pause: Unit

  def playing: Boolean

  def volume: Float
  def volume_=(volume: Float)

  def loop: Boolean
  def loop_=(loop: Boolean)

  def pitch: Float
  def pitch_=(pitch: Float)

  def close(): Unit = {}
}

abstract class Source extends AbstractSource

abstract class Source3D extends AbstractSource {
  def position: Vector3f
  def position_=(position: Vector3f)
}