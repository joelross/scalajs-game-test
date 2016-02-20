package games

import java.io.Closeable
import java.util.concurrent.LinkedBlockingQueue

import scala.concurrent.{ ExecutionContext, Promise, Future }

import org.lwjgl.glfw.{ GLFW, GLFWErrorCallback }

class GLFWManager(mainThread: Thread) extends Closeable {
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
  
  private def isCurrentlyMainThread: Boolean = Thread.currentThread == mainThread
  
  private def checkCurrentThread(): Unit = {
    if (!isCurrentlyMainThread) throw new RuntimeException("This method can only be called from the main thread")
  }
  
  private def executeNow(runnable: Runnable): Unit = {
    try { runnable.run() }
    catch { case t: Throwable => mainExecutionContext.reportFailure(t) }
  }
  
  // May only be called from the main thread, blocking method
  def loop(): Unit = {
    checkCurrentThread()
  
    continueLoop = true
    while(continueLoop) {
      val current = pendingRunnables.take()
      try { current.run() }
      catch { case t: Throwable => mainExecutionContext.reportFailure(t) }
    }
  }
  
  def stopLoop(): Unit = {
    pendingRunnables.put(new Runnable() {
      def run(): Unit = {
        continueLoop = false
      }
    })
  }
  
  // May only be called from the main thread
  def flushPendingMainIOEvents(): Unit = {
    checkCurrentThread()
  
    var current: Runnable = null
    while ({ current = pendingRunnables.poll(); current } != null) {
      executeNow(current)
    }
  }

  def close(): Unit = {
    GLFW.glfwTerminate()
    errorCallback.release()
  }
  
  val mainExecutionContext = new ExecutionContext() {
    def execute(runnable: Runnable): Unit = {
      if (isCurrentlyMainThread) {
        executeNow(runnable)
      } else {
        pendingRunnables.put(runnable)
      }
    }
    def reportFailure(cause: Throwable): Unit = {
      ExecutionContext.defaultReporter(cause)
    }
  }
}
