package games.audio

import games.Resource

abstract class Context {
  def createBufferedData(res: Resource): BufferedData
  def createStreamingData(res: Resource): StreamingData
  def createRawData(): RawData
}