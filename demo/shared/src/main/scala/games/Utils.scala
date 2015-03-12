package games

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import java.nio.ByteBuffer
import games.opengl.GLES2
import games.opengl.Token
import scala.annotation.implicitNotFound

case class FrameEvent(elapsedTime: Float)

trait FrameListener {
  def onCreate(): Unit
  def onChange(width: Int, height: Int): Unit
  def continue(): Boolean
  def onDraw(fe: FrameEvent): Unit
  def onClose(): Unit
}

trait UtilsRequirements {
  def getBinaryDataFromResource(res: games.Resource): scala.concurrent.Future[java.nio.ByteBuffer]
  def getTextDataFromResource(res: games.Resource): scala.concurrent.Future[Array[String]]
  def loadTexture2DFromResource(res: games.Resource, texture: games.opengl.Token.Texture, preload: => Boolean = true)(implicit gl: games.opengl.GLES2): scala.concurrent.Future[Unit]
  def startFrameListener(fl: games.FrameListener): Unit
}

object Utils extends UtilsImpl {}