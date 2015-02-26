package games.audio

import scala.concurrent.Future

abstract sealed class AbstractSource {
  def play: Unit
  def pause: Unit
  
  def volume: Float
  def volume_=(volume: Float)
  
  def loop: Boolean
  def loop_=(loop: Boolean)
  
  def playbackRate: Float
  def playbackRate_=(playbackRate: Float)
}

abstract class Source extends AbstractSource {
  
}

abstract class Source3D extends AbstractSource {
  
}