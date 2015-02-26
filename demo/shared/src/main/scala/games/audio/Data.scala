package games.audio

import scala.concurrent.Future

sealed abstract class Data {
  def createSource: Future[Source]
  def createSource3D: Future[Source3D]
}

abstract class BufferedData extends Data
abstract class StreamingData extends Data
abstract class RawData extends Data