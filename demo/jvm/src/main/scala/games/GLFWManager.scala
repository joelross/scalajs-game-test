package games

import java.io.Closeable

import org.lwjgl.glfw.{ GLFW, GLFWErrorCallback }

class GLFWManager extends Closeable {
  // Init
  val (errorCallback) = {
    val errorCallback = GLFWErrorCallback.createPrint(System.err)
    GLFW.glfwSetErrorCallback(errorCallback)
    
    val initSuccess = GLFW.glfwInit()
    require(initSuccess == GLFW.GLFW_TRUE)
    
    (errorCallback)
  }
  
  // May only be called from the main thread, blocking method
  def handleMainIOEvents(): Unit = {
  }
  
  // May only be called from the main thread
  def flushPendingMainIOEvents(): Unit = {
  }

  def close(): Unit = {
    GLFW.glfwTerminate()
    errorCallback.release()
  }
}
