package games.input

import org.lwjgl.input.{ Mouse => LWJGLMouse }

class MouseLWJGL() extends Mouse {
  def deltaPosition: games.input.Position = ???
  def locked: Boolean = ???
  def locked_=(locked: Boolean): Unit = ???
  def position: games.input.Position = ???
}