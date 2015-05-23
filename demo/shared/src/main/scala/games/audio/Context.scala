package games.audio

import scala.concurrent.Future

import games.Resource
import games.math.Vector3f
import java.io.Closeable

import java.nio.ByteBuffer

abstract sealed class Format

object Format {
  case object Float32 extends Format
}

abstract class Context extends Closeable {
  def prepareStreamingData(res: Resource): Future[games.audio.Data]
  def prepareBufferedData(res: Resource): Future[games.audio.BufferedData]
  def prepareRawData(data: ByteBuffer, format: Format, channels: Int, freq: Int): Future[games.audio.BufferedData]

  def createSource(): Source
  def createSource3D(): Source3D

  def listener: Listener

  def volume: Float
  def volume_=(volume: Float)

  def close(): Unit = {}
}

sealed trait Spatial {
  def position: Vector3f
  def position_=(position: Vector3f)
}

abstract class Listener extends Closeable with Spatial {
  def up: Vector3f

  def orientation: Vector3f

  def setOrientation(orientation: Vector3f, up: Vector3f): Unit

  def close(): Unit = {}
}

abstract class Data extends Closeable {
  def attach(source: AbstractSource): Future[games.audio.Player]

  def close(): Unit = {}
}

abstract class BufferedData extends Data {
  def attachNow(source: AbstractSource): games.audio.Player
}

abstract class Player extends Closeable {
  def playing: Boolean
  def playing_=(playing: Boolean): Unit

  def volume: Float
  def volume_=(volume: Float): Unit

  def loop: Boolean
  def loop_=(loop: Boolean): Unit

  def pitch: Float
  def pitch_=(pitch: Float): Unit

  def close(): Unit = {
    this.playing = false
  }
}

sealed abstract class AbstractSource extends Closeable {
  def close(): Unit = {}
}
abstract class Source extends AbstractSource
abstract class Source3D extends AbstractSource with Spatial
