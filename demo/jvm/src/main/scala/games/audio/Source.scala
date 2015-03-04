package games.audio

import scala.concurrent._

import org.lwjgl.openal.AL10

class ALBufferedSource private[games] (ctx: ALContext, alBuffer: Int) extends Source {
  
  private def init() = {
    val alSource = AL10.alGenSources()
    AL10.alSourcei(alSource, AL10.AL_BUFFER, alBuffer)
    alSource
  }
  
  private val alSource = init()
  
  def loop: Boolean = AL10.alGetSourcei(alSource, AL10.AL_LOOPING) == AL10.AL_TRUE
  def loop_=(loop: Boolean): Unit = AL10.alSourcei(alSource, AL10.AL_LOOPING, if (loop) AL10.AL_TRUE else AL10.AL_FALSE)
  def pause: Unit = AL10.alSourcePause(alSource)
  def play: Unit = AL10.alSourcePlay(alSource)
  def pitch: Float = AL10.alGetSourcef(alSource, AL10.AL_PITCH)
  def pitch_=(pitch: Float): Unit = AL10.alSourcef(alSource, AL10.AL_PITCH, pitch)
  def volume: Float = AL10.alGetSourcef(alSource, AL10.AL_GAIN)
  def volume_=(volume: Float): Unit = AL10.alSourcef(alSource, AL10.AL_GAIN, volume)
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

