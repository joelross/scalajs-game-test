package games.opengl

import org.lwjgl.glfw.{ GLFW, GLFWKeyCallback, GLFWVidMode }
import org.lwjgl.system.MemoryUtil.{ NULL => LWJGL_NULL }

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration.{ Duration }
import scala.util.{ Success, Failure }

case class GLFWWindowSettings(width: Int, height: Int, fullscreen: Boolean = false, vsync: Boolean = true, title: String = "OpenGL window")

class GLFWWindow(optSettings: Option[GLFWWindowSettings]) extends Display {

  private val keyCallback: GLFWKeyCallback = new GLFWKeyCallback() {
    def invoke(window: Long, key: Int, scanCode: Int, action: Int, mods: Int): Unit = {
      println("### Key callback")
      // TODO
    }
  }

  // Init
  private val windowPtr: Long = {
    val futureWindowPtr = Future {
      val primaryMonitorPtr = GLFW.glfwGetPrimaryMonitor()
      val primaryVidMode = GLFW.glfwGetVideoMode(primaryMonitorPtr)
      
      val settings = optSettings.getOrElse(GLFWWindowSettings(primaryVidMode.width() / 2, primaryVidMode.height() / 2))
    
      GLFW.glfwDefaultWindowHints()
      GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_ES_API)
      GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 2)
      GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 0)
      GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE)
      GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE)

      val width: Int = settings.width
      val height: Int = settings.height
      val title: java.lang.CharSequence = settings.title
      val monitorPtr: Long = if(settings.fullscreen) primaryMonitorPtr else LWJGL_NULL
      val sharedContextPtr: Long = LWJGL_NULL

      val windowPtr = GLFW.glfwCreateWindow(width, height, title, monitorPtr, sharedContextPtr)
      require(windowPtr != LWJGL_NULL)
      GLFW.glfwMakeContextCurrent(windowPtr)
      
      if(settings.vsync) GLFW.glfwSwapInterval(1)
      
      val posx = (primaryVidMode.width() - width) / 2
      val posy = (primaryVidMode.height() - height) / 2
      GLFW.glfwSetWindowPos(windowPtr, posx, posy)

      GLFW.glfwSetKeyCallback(windowPtr, keyCallback)
      
      GLFW.glfwMakeContextCurrent(LWJGL_NULL)
      windowPtr
    }(games.JvmUtils.getGLFWManager.mainThreadExecutionContext)
    
    val windowPtr: Long = Await.ready(futureWindowPtr, Duration.Inf).value.get match {
      case Success(ptr) => ptr
      case Failure(exc) => throw new RuntimeException("Could not create window", exc)
    }
    
    GLFW.glfwMakeContextCurrent(windowPtr)

    windowPtr
  }

  override def close(): Unit = {
    super.close()
    GLFW.glfwDestroyWindow(windowPtr)
    keyCallback.release()
  }

  def fullscreen: Boolean = ???
  def fullscreen_=(fullscreen: Boolean): Unit = ???

  def width: Int = ???
  def height: Int = ???
}
