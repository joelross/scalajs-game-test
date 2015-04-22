package games.input

import java.io.Closeable

case class Touch(identifier: Int, position: Position)

abstract sealed class TouchEvent
case class TouchStart(contact: Touch)
case class TouchEnd(contact: Touch)

abstract class Touchpad extends Closeable {
  def touches: Seq[Touch]

  def nextEvent(): Option[TouchEvent]

  def close(): Unit = {}
}