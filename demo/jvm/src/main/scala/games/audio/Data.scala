package games.audio

class ALBufferedData private[games] () extends BufferedData {
  def createSource: scala.concurrent.Future[games.audio.Source] = ???
  def createSource3D: scala.concurrent.Future[games.audio.Source3D] = ???
}

class ALStreamingData private[games] () extends BufferedData {
  def createSource: scala.concurrent.Future[games.audio.Source] = ???
  def createSource3D: scala.concurrent.Future[games.audio.Source3D] = ???
}