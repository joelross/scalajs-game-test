package games.audio

abstract sealed class AbstractSource {
  def play: Unit
  def pause: Unit
  
  def volume: Float
  def volume_=(volume: Float)
  
  def loop: Boolean
  def loop_=(loop: Boolean)
  
  def pitch: Float
  def pitch_=(pitch: Float)
}

abstract class Source extends AbstractSource

abstract class Source3D extends AbstractSource {
  // TODO 3d positioning
}