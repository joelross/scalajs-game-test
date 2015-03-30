package games.audio

import games.Resource
import games.math.Vector3f
import java.io.Closeable

import java.nio.ByteBuffer

abstract sealed class Format

object Format {
  case object Float32 extends Format
}

abstract class Context extends Closeable {
  def createBufferedData(res: Resource): BufferedData
  def createStreamingData(res: Resource): StreamingData
  def createRawData(data: ByteBuffer, format: Format, channels: Int, freq: Int): RawData

  def listener: Listener

  def volume: Float
  def volume_=(volume: Float)

  def close(): Unit = {}
}

abstract class Listener extends Closeable {
  def position: Vector3f
  def position_=(position: Vector3f)

  def up: Vector3f

  def orientation: Vector3f

  def setOrientation(orientation: Vector3f, up: Vector3f): Unit

  def close(): Unit = {}
}