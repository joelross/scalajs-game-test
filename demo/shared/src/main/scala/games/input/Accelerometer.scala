package games.input

import java.io.Closeable

abstract class Accelerometer extends Closeable {
  def current(): Option[games.math.Vector3f]

  def close(): Unit = {}
}
