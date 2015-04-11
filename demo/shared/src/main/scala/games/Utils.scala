package games

import scala.concurrent.{ Future, Promise, ExecutionContext }
import java.nio.ByteBuffer
import games.opengl.GLES2
import games.opengl.Token

case class FrameEvent(elapsedTime: Float)

trait FrameListener {
  final val loopExecutionContext: ExecutionContext = Utils.getLoopThreadExecutionContext()

  def context: GLES2

  def onCreate(): Option[Future[Unit]]
  def continue(): Boolean
  def onDraw(fe: FrameEvent): Unit
  def onClose(): Unit
}

trait UtilsRequirements {
  def getLoopThreadExecutionContext(): ExecutionContext
  def getBinaryDataFromResource(res: games.Resource)(implicit ec: ExecutionContext): scala.concurrent.Future[java.nio.ByteBuffer]
  def getTextDataFromResource(res: games.Resource)(implicit ec: ExecutionContext): scala.concurrent.Future[String]
  def loadTexture2DFromResource(res: games.Resource, texture: games.opengl.Token.Texture, gl: games.opengl.GLES2, openglExecutionContext: ExecutionContext, preload: => Boolean = true)(implicit ec: ExecutionContext): scala.concurrent.Future[Unit]
  def startFrameListener(fl: games.FrameListener): Unit
}

object Utils extends UtilsImpl {
  /**
   * Trivial ExecutionContext that simply execute the Runnable immediately in the same thread
   */
  val immediateExecutionContext: ExecutionContext = new ExecutionContext {
    def execute(runnable: Runnable): Unit = {
      try { runnable.run() }
      catch { case t: Throwable => this.reportFailure(t) }
    }
    def reportFailure(cause: Throwable): Unit = ExecutionContext.defaultReporter(cause)
  }

  /**
   * Trivial function that cut a string into lines
   */
  def lines(text: String): Array[String] = {
    text.replaceAll("\r", "").split("\n")
  }

  /**
   * Function reducing a Future[Future[T]] to a Future[T]
   */
  def reduceFuture[T](orig: Future[Future[T]])(implicit ec: ExecutionContext): Future[T] = orig.flatMap(identity)
}

package object input {
  type Key = Int
}
