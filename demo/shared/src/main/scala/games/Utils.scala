package games

import scala.concurrent.{ Future, Promise, ExecutionContext }
import java.nio.ByteBuffer
import games.opengl.GLES2
import games.opengl.Token

case class FrameEvent(elapsedTime: Float)

trait FrameListener {
  val loopExecutionContext: ExecutionContext = Utils.getLoopThreadExecutionContext()

  def onCreate(): Future[Unit]
  def onDraw(fe: FrameEvent): Boolean
  def onClose(): Unit
}

trait UtilsRequirements {
  private[games] def getLoopThreadExecutionContext(): ExecutionContext
  def getBinaryDataFromResource(res: games.Resource)(implicit ec: ExecutionContext): scala.concurrent.Future[java.nio.ByteBuffer]
  def getTextDataFromResource(res: games.Resource)(implicit ec: ExecutionContext): scala.concurrent.Future[String]
  def loadTexture2DFromResource(res: games.Resource, texture: games.opengl.Token.Texture, gl: games.opengl.GLES2, openglExecutionContext: ExecutionContext)(implicit ec: ExecutionContext): scala.concurrent.Future[Unit]
  def startFrameListener(fl: games.FrameListener): Unit
}

object Utils extends UtilsImpl
