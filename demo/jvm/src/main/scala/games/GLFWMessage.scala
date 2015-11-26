package games

case class GLFWWindow(pointer: Long)
case class GLFWMonitor(pointer: Long)

case class WindowSettings(width: Int, height: Int, fullscreen: Option[GLFWMonitor], vsync: Boolean)

sealed trait GLFWMessage
abstract class GLFWWindowMessage(window: GLFWWindow) extends GLFWMessage
case class CreateWindow(settings: WindowSettings, window: GLFWWindow) extends GLFWWindowMessage(window)
case class SetWindow(settings: WindowSettings, window: GLFWWindow) extends GLFWWindowMessage(window)
case class MouseLock(enabled: Boolean, window: GLFWWindow) extends GLFWWindowMessage(window)
