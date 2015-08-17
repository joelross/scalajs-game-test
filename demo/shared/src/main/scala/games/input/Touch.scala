package games.input

import java.io.Closeable

case class Touch(identifier: Int, position: Position)

case class TouchEvent(data: Touch, start: Boolean)

abstract class Touchscreen extends Closeable {
  def touches: Seq[Touch]

  def nextEvent(): Option[TouchEvent]

  def close(): Unit = {}
}
