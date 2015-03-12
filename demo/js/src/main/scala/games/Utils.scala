package games

import scala.concurrent.{ Future, ExecutionContext }

object JsUtils {
  private var relativeResourcePath: Option[String] = None

  def pathForResource(res: Resource): String = relativeResourcePath match {
    case Some(path) => path + res.name
    case None       => throw new RuntimeException("Relative path must be defined before calling pathForResource")
  }

  def setResourcePath(path: String): Unit = {
    relativeResourcePath = Some(path)
  }
}

trait UtilsImpl extends UtilsRequirements {
  def getBinaryDataFromResource(res: games.Resource)(implicit ec: ExecutionContext): scala.concurrent.Future[java.nio.ByteBuffer] = ???
  def getTextDataFromResource(res: games.Resource)(implicit ec: ExecutionContext): scala.concurrent.Future[Array[String]] = ???
  def loadTexture2DFromResource(res: games.Resource, texture: games.opengl.Token.Texture, preload: => Boolean = true)(implicit gl: games.opengl.GLES2, ec: ExecutionContext): scala.concurrent.Future[Unit] = ???
  def startFrameListener(fl: games.FrameListener): Unit = ???
}