package games

case class Window(pointer: Long)
case class Monitor(pointer: Long)

case class WindowSettings(width: Int, height: Int, fullscreen: Option[Monitor], vsync: Boolean)

sealed trait GLFWMessage
abstract class WindowMessage(window: Window) extends GLFWMessage
case class CreateGLES20Window(settings: WindowSettings, window: Window) extends WindowMessage(window)
case class setWindow(settings: WindowSettings, window: Window) extends WindowMessage(window)
