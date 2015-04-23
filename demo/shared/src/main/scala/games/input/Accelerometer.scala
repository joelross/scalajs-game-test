package games.input

import java.io.Closeable

abstract class Accelerometer extends Closeable {
  def current(): games.math.Vector3f

  def close(): Unit = {}
}