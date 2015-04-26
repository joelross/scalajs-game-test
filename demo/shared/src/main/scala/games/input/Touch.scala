package games.input

import java.io.Closeable

case class Touch(identifier: Int, position: Position)

abstract sealed class TouchEvent
case class TouchStart(data: Touch) extends TouchEvent
case class TouchEnd(data: Touch) extends TouchEvent

abstract class Touchpad extends Closeable {
  def touches: Seq[Touch]

  def nextEvent(): Option[TouchEvent]

  def close(): Unit = {}
}