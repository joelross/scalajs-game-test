package games.audio

class ALContext extends Context {
  def createBufferedData(res: games.Resource): games.audio.BufferedData = ???
  def createRawData(): games.audio.RawData = ???
  def createStreamingData(res: games.Resource): games.audio.StreamingData = ???
}