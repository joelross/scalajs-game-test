package games.audio

import scala.concurrent.Future

import java.io.Closeable

abstract class Data extends Closeable {
  def createSource(): Future[games.audio.Source]
  def createSource3D(): Future[games.audio.Source3D]

  def close(): Unit = {}
}
