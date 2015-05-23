package games.audio

import games.math.Vector3f

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import java.nio.{ ByteBuffer, ByteOrder }

import org.lwjgl.openal.AL10
import org.lwjgl.openal.Util

sealed trait ALAbstractSource extends AbstractSource {
  override def close(): Unit = {
    super.close()
  }
}

class ALSource(val ctx: ALContext) extends Source with ALAbstractSource {
  ctx.registerSource(this)

  override def close(): Unit = {
    super.close()

    ctx.unregisterSource(this)
  }
}

class ALSource3D(val ctx: ALContext) extends Source3D with ALAbstractSource {
  def position: games.math.Vector3f = {
    positionBuffer.rewind()
    val ret = new Vector3f
    ret.load(positionBuffer)
    ret
  }
  def position_=(position: games.math.Vector3f): Unit = {
    positionBuffer.rewind()
    position.store(positionBuffer)
    positionBuffer.rewind()
    for (player <- this.players) {
      val alSource = player.asInstanceOf[ALPlayer].alSource
      AL10.alSource(alSource, AL10.AL_POSITION, positionBuffer)
    }
    Util.checkALError()
  }

  private val positionBuffer = ByteBuffer.allocateDirect(3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

  ctx.registerSource(this)

  override def close(): Unit = {
    super.close()

    ctx.unregisterSource(this)
  }
}

sealed trait ALData extends Data {
  private[games] val ctx: ALContext

  override def close(): Unit = {
    super.close()
  }
}

class ALBufferData(val ctx: ALContext, alBuffer: Int) extends BufferedData with ALData {
  ctx.registerData(this)

  def attach(source: games.audio.AbstractSource): scala.concurrent.Future[games.audio.Player] = Future.successful(this.attachNow(source))
  def attachNow(source: games.audio.AbstractSource): games.audio.Player = {
    val alSource = AL10.alGenSources()
    AL10.alSourcei(alSource, AL10.AL_BUFFER, alBuffer)

    val alAudioSource = source.asInstanceOf[ALAbstractSource]
    val ret = new ALBufferPlayer(this, alAudioSource, alSource)
    Util.checkALError()
    ret
  }

  override def close(): Unit = {
    super.close()

    ctx.unregisterData(this)
  }
}

class ALStreamingData(val ctx: ALContext) extends Data with ALData {
  ctx.registerData(this)

  def attach(source: games.audio.AbstractSource): scala.concurrent.Future[games.audio.Player] = ???

  override def close(): Unit = {
    super.close()

    ctx.unregisterData(this)
  }
}

sealed trait ALPlayer extends Player {
  private[games] val alSource: Int

  private[games] def applyChangedVolume(): Unit
}

abstract class ALBasicPlayer(val data: ALData, val source: ALAbstractSource, val alSource: Int) extends ALPlayer {
  source.registerPlayer(this)
  data.registerPlayer(this)

  private var thisVolume = 1f

  private[games] def applyChangedVolume(): Unit = {
    val curVolume = data.ctx.masterVolume * thisVolume
    AL10.alSourcef(alSource, AL10.AL_GAIN, curVolume)
    Util.checkALError()
  }

  def volume: Float = thisVolume
  def volume_=(volume: Float): Unit = {
    thisVolume = volume
    applyChangedVolume
  }

  override def close(): Unit = {
    super.close()

    source.unregisterPlayer(this)
    data.unregisterPlayer(this)

    AL10.alDeleteSources(alSource)
    Util.checkALError()
  }
}

class ALBufferPlayer(override val data: ALBufferData, override val source: ALAbstractSource, override val alSource: Int) extends ALBasicPlayer(data, source, alSource) {
  def loop: Boolean = {
    val ret = AL10.alGetSourcei(alSource, AL10.AL_LOOPING) == AL10.AL_TRUE
    Util.checkALError()
    ret
  }
  def loop_=(loop: Boolean): Unit = {
    AL10.alSourcei(alSource, AL10.AL_LOOPING, if (loop) AL10.AL_TRUE else AL10.AL_FALSE)
    Util.checkALError()
  }

  def pitch: Float = {
    val ret = AL10.alGetSourcef(alSource, AL10.AL_PITCH)
    Util.checkALError()
    ret
  }
  def pitch_=(pitch: Float): Unit = {
    AL10.alSourcef(alSource, AL10.AL_PITCH, pitch)
    Util.checkALError()
  }

  def playing: Boolean = {
    val ret = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING
    Util.checkALError()
    ret
  }
  def playing_=(playing: Boolean): Unit = if (playing) {
    AL10.alSourcePlay(alSource)
    Util.checkALError()
  } else {
    AL10.alSourcePause(alSource)
    Util.checkALError()
  }

  override def close(): Unit = {
    super.close()
  }
}

class ALStreamingPlayer(override val data: ALStreamingData, override val source: ALAbstractSource, override val alSource: Int) extends ALBasicPlayer(data, source, alSource) {
  def loop: Boolean = ???
  def loop_=(loop: Boolean): Unit = ???

  def pitch: Float = ???
  def pitch_=(pitch: Float): Unit = ???

  def playing: Boolean = ???
  def playing_=(playing: Boolean): Unit = ???

  override def close(): Unit = {
    super.close()
  }
}
