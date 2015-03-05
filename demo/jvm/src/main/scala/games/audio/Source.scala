package games.audio

import scala.concurrent._
import org.lwjgl.openal.AL10
import org.lwjgl.openal.Util
import java.io.InputStream

import games.JvmResourceUtil
import games.Resource

class ALBufferedSource private[games] (ctx: ALContext, alBuffer: Int) extends Source {

  private def init() = {
    val alSource = AL10.alGenSources()
    AL10.alSourcei(alSource, AL10.AL_BUFFER, alBuffer)

    Util.checkALError()
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

  override def close(): Unit = {
    AL10.alDeleteSources(alSource)
    Util.checkALError()
  }
}

class ALStreamingSource private[games] (ctx: ALContext, res: Resource) extends Source {

  private val converter = new FixedSigned16Converter

  private def init() = {
    val alSource = AL10.alGenSources()

    val streamingThread = new Thread(new Runnable() {
      def run(): Unit = {
        var running = true
        var decoder = new VorbisDecoder(JvmResourceUtil.streamForResource(res), converter)

        while (running) {

        }

        decoder.close()
      }
    })

    Util.checkALError()
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

  override def close(): Unit = {
    // TODO close the streamingThread

    AL10.alDeleteSources(alSource)
    Util.checkALError()
  }

  private val promiseReady = Promise[Unit]
  private[games] val ready = promiseReady.future
}

