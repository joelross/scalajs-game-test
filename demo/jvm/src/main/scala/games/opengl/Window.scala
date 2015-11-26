package games.opengl

import org.lwjgl.glfw.{ GLFW, GLFWKeyCallback }
import org.lwjgl.system.MemoryUtil.{ NULL => LWJGL_NULL }

case class GLFWWindowSettings(width: Int, height: Int, fullscreen: Boolean, vsync: Boolean)

class GLFWWindow(glMajor: Int, glMinor: Int, settings: Option[GLFWWindowSettings]) extends Display {
  // Init
  val (windowPointer, keyCallback): (Long, GLFWKeyCallback) = {
    
    val keyCallback = new GLFWKeyCallback() {
      def invoke(window: Long, key: Int, scanCode: Int, action: Int, mods: Int): Unit = {
        println("### Key callback")
        // TODO
      }
    }

    //println("Hello LWJGL " + org.lwjgl.Version.getVersion() + "!");

    GLFW.glfwDefaultWindowHints()
    GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_ES_API)
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 2)
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 0)
    GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE)
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE)

    val width: Int = 640
    val height: Int = 480
    val title: java.lang.CharSequence = "OpenGL window"
    val monitorPointer: Long = LWJGL_NULL
    val sharedContextPointer: Long = LWJGL_NULL

    val windowPointer = GLFW.glfwCreateWindow(width, height, title, monitorPointer, sharedContextPointer)
    require(windowPointer != LWJGL_NULL)

    GLFW.glfwSetKeyCallback(windowPointer, keyCallback)

    GLFW.glfwMakeContextCurrent(windowPointer)

    val vsync = false
    if (vsync) GLFW.glfwSwapInterval(1)

    (windowPointer, keyCallback)
  }

  override def close(): Unit = {
    super.close()
    GLFW.glfwDestroyWindow(windowPointer)
    keyCallback.release()
    
  }

  def fullscreen: Boolean = ???
  def fullscreen_=(fullscreen: Boolean): Unit = ???

  def width: Int = ???
  def height: Int = ???
}
