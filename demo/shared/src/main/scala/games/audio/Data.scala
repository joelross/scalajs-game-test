package games.audio

import scala.concurrent.Future

import java.io.Closeable

sealed abstract class Data extends Closeable {
  def createSource: Future[Source]
  def createSource3D: Future[Source3D]
  
  def close(): Unit = {}
}

abstract class BufferedData extends Data
abstract class StreamingData extends Data
abstract class RawData extends Data