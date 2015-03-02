package games.audio

class ALBufferedSource private[games] () extends Source {
  def loop: Boolean = ???
  def loop_=(loop: Boolean): Unit = ???
  def pause: Unit = ???
  def play: Unit = ???
  def pitch: Float = ???
  def pitch_=(pitch: Float): Unit = ???
  def volume: Float = ???
  def volume_=(volume: Float): Unit = ???
}

class ALStreamingSource private[games] () extends Source {
  def loop: Boolean = ???
  def loop_=(loop: Boolean): Unit = ???
  def pause: Unit = ???
  def play: Unit = ???
  def pitch: Float = ???
  def pitch_=(pitch: Float): Unit = ???
  def volume: Float = ???
  def volume_=(volume: Float): Unit = ???
}

