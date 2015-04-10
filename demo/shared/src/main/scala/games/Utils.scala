package games

import scala.concurrent.{ Future, Promise, ExecutionContext }
import java.nio.ByteBuffer
import games.opengl.GLES2
import games.opengl.Token

case class FrameEvent(elapsedTime: Float)

trait FrameListener {
  def context: GLES2

  def onCreate(): Option[Future[Unit]]
  def continue(): Boolean
  def onDraw(fe: FrameEvent): Unit
  def onClose(): Unit
}

trait UtilsRequirements {
  def getBinaryDataFromResource(res: games.Resource)(implicit ec: ExecutionContext): scala.concurrent.Future[java.nio.ByteBuffer]
  def getTextDataFromResource(res: games.Resource)(implicit ec: ExecutionContext): scala.concurrent.Future[String]
  def loadTexture2DFromResource(res: games.Resource, texture: games.opengl.Token.Texture, preload: => Boolean = true)(implicit gl: games.opengl.GLES2, ec: ExecutionContext): scala.concurrent.Future[Unit]
  def startFrameListener(fl: games.FrameListener): Unit
}

object Utils extends UtilsImpl {
  def lines(text: String): Array[String] = {
    text.replaceAll("\r", "").split("\n")
  }

  def reduceFuture[T](orig: Future[Future[T]])(implicit ec: ExecutionContext): Future[T] = {
    val promise = Promise[T]

    orig.onSuccess {
      case inner =>
        inner.onSuccess { case value => promise.success(value) }
        inner.onFailure { case t => promise.failure(t) }
    }
    orig.onFailure { case t => promise.failure(t) }

    promise.future
  }
}

package object input {
  type Key = Int
}
