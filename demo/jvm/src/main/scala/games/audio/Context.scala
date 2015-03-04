package games.audio

import org.lwjgl.openal.AL

class ALContext extends Context {
  AL.create()
  
  def createBufferedData(res: games.Resource): games.audio.BufferedData = new ALBufferedData(this, res)
  def createRawData(): games.audio.RawData = ???
  def createStreamingData(res: games.Resource): games.audio.StreamingData = new ALStreamingData(this, res)
  
  override def close(): Unit = {
    AL.destroy()
  }
}