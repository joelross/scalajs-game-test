package games.audio

import games.Resource
import games.math.Vector3f
import java.io.Closeable

abstract class Context extends Closeable {
  def createBufferedData(res: Resource): BufferedData
  def createStreamingData(res: Resource): StreamingData
  def createRawData(): RawData

  def listener: Listener

  def close(): Unit = {}
}

abstract class Listener extends Closeable {
  def position: Vector3f
  def position_=(position: Vector3f)

  def up: Vector3f
  def up_=(up: Vector3f)

  def orientation: Vector3f
  def orientation_=(orientation: Vector3f)

  def close(): Unit = {}
}