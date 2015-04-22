package games.input

import java.io.Closeable

case class Acceleration(x: Float, y: Float, z: Float)

abstract class Accelerometer extends Closeable {
  def current(): Acceleration

  def close(): Unit = {}
}