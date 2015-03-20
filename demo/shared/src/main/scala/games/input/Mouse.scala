package games.input

import java.io.Closeable

object Mouse {
  private[games]type ButtonMapper[T] = BiMap[Button, T]
}

sealed abstract class Button

object Button {
  case object Left extends Button
  case object Right extends Button
  case object Middle extends Button
  case class Aux(num: Int) extends Button
}

sealed abstract class Wheel

object Wheel {
  case object Up extends Wheel
  case object Down extends Wheel
  case object Left extends Wheel
  case object Right extends Wheel
}

case class Position(x: Int, y: Int)
abstract sealed class MouseEvent
case class ButtonEvent(button: Button, down: Boolean) extends MouseEvent
case class WheelEvent(direction: Wheel) extends MouseEvent

abstract class Mouse extends Closeable {
  def position: Position
  def deltaPosition: Position

  def locked: Boolean
  def locked_=(locked: Boolean): Unit

  def isButtonDown(button: Button): Boolean
  def nextEvent(): Option[MouseEvent]

  def isInside(): Boolean

  def close(): Unit = {}
}