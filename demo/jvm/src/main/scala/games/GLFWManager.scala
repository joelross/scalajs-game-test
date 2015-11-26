package games

import java.io.Closeable
import java.util.concurrent.LinkedBlockingQueue

import scala.concurrent.{ ExecutionContext, Promise, Future }

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
  
  private var continueLoop = false
  private val pendingRunnables = new LinkedBlockingQueue[Runnable]
  
  // May only be called from the main thread, blocking method
  def loop(): Unit = {
    continueLoop = true
    while(continueLoop) {
      val current = pendingRunnables.take()
      try { current.run() }
      catch { case t: Throwable => mainThreadExecutionContext.reportFailure(t) }
    }
  }
  
  def stopLoop(): Unit = {
    continueLoop = false
    // Put an empty Runnable to awake the blocking list
    pendingRunnables.put(new Runnable() {
      def run(): Unit = {}
    })
  }
  
  // May only be called from the main thread
  def flushPendingMainIOEvents(): Unit = {
    var current: Runnable = null
    while ({ current = pendingRunnables.poll(); current } != null) {
      try { current.run() }
      catch { case t: Throwable => mainThreadExecutionContext.reportFailure(t) }
    }
  }

  def close(): Unit = {
    GLFW.glfwTerminate()
    errorCallback.release()
  }
  
  val mainThreadExecutionContext = new ExecutionContext() {
    def execute(runnable: Runnable): Unit = {
      pendingRunnables.put(runnable)
    }
    def reportFailure(cause: Throwable): Unit = {
      ExecutionContext.defaultReporter(cause)
    }
  }
}
