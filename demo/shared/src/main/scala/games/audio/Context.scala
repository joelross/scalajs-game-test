package games.audio

import games.Resource
import java.io.Closeable

abstract class Context extends Closeable {
  def createBufferedData(res: Resource): BufferedData
  def createStreamingData(res: Resource): StreamingData
  def createRawData(): RawData
  
  def close(): Unit = {}
}