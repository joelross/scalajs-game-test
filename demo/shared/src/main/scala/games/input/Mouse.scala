package games.input

import java.io.Closeable

sealed abstract class MouseButton

object MouseButton {
  case object Left extends MouseButton
  case object Right extends MouseButton
  case object Middle extends MouseButton
}

case class Position(x: Int, y: Int)

case class MouseButtonEvent(button: MouseButton, down: Boolean)

abstract class Mouse extends Closeable {
  def position: Position
  def deltaPosition: Position

  def locked: Boolean
  def locked_=(locked: Boolean): Unit

  def isButtonDown(button: MouseButton): Boolean
  def nextEvent(): Option[MouseButtonEvent]

  def close(): Unit = {}
}