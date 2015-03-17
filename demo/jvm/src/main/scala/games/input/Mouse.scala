package games.input

import org.lwjgl.input.{ Mouse => LWJGLMouse }

object MouseLWJGL {
  val mapper = new Mouse.ButtonMapper[Int](
    (Button.Left, 0),
    (Button.Right, 1),
    (Button.Middle, 2))

  private def getForLocal(button: Button): Int = button match {
    case Button.Aux(num) => num
    case _ => MouseLWJGL.mapper.getForLocal(button) match {
      case Some(num) => num
      case None      => throw new RuntimeException("No known LWJGL code for button " + button)
    }
  }

  private def getForRemote(eventButton: Int): Button = MouseLWJGL.mapper.getForRemote(eventButton) match {
    case Some(button) => button
    case None         => Button.Aux(eventButton)
  }
}

class MouseLWJGL() extends Mouse {
  LWJGLMouse.create()

  override def close(): Unit = {
    LWJGLMouse.destroy()
  }

  def position: games.input.Position = {
    val x = LWJGLMouse.getX()
    val y = LWJGLMouse.getY()
    Position(x, org.lwjgl.opengl.Display.getDisplayMode().getHeight() - y)
  }
  def deltaPosition: games.input.Position = {
    val dx = LWJGLMouse.getDX()
    val dy = LWJGLMouse.getDY()
    Position(dx, -dy)
  }

  def locked: Boolean = LWJGLMouse.isGrabbed()
  def locked_=(locked: Boolean): Unit = LWJGLMouse.setGrabbed(locked)

  def isButtonDown(button: games.input.Button): Boolean = LWJGLMouse.isButtonDown(MouseLWJGL.getForLocal(button))
  def nextEvent(): Option[games.input.MouseEvent] = {
    if (LWJGLMouse.next()) {
      val eventButton = LWJGLMouse.getEventButton()
      val eventWheel = LWJGLMouse.getEventDWheel()

      if (eventButton >= 0) {
        val button = MouseLWJGL.getForRemote(eventButton)
        val down = LWJGLMouse.getEventButtonState
        Some(ButtonEvent(button, down))
      } else if (eventWheel != 0) {
        if (eventWheel > 0) Some(WheelEvent(Wheel.Up))
        else Some(WheelEvent(Wheel.Down))
      } else nextEvent() // unknown event, skip to the next
    } else None
  }

  def isInside(): Boolean = LWJGLMouse.isInsideWindow()
}