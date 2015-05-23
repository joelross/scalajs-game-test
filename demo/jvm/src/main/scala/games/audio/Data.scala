package games.audio

import games.Resource
import games.math.Vector3f
import games.JvmUtils

import scala.concurrent.{ Promise, Future }
import scala.concurrent.ExecutionContext.Implicits.global

import java.nio.{ ByteBuffer, ByteOrder }
import java.io.EOFException

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

  override private[games] def registerPlayer(player: Player): Unit = {
    super.registerPlayer(player)

    // Preload buffer
    val alSource = player.asInstanceOf[ALPlayer].alSource
    AL10.alSource(alSource, AL10.AL_POSITION, positionBuffer)
    AL10.alGetSource(alSource, AL10.AL_POSITION, positionBuffer)
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

class ALStreamingData(val ctx: ALContext, res: Resource) extends Data with ALData {
  ctx.registerData(this)

  def attach(source: games.audio.AbstractSource): scala.concurrent.Future[games.audio.Player] = {
    val alSource = AL10.alGenSources()
    val alAudioSource = source.asInstanceOf[ALAbstractSource]
    val player = new ALStreamingPlayer(this, alAudioSource, alSource, res)
    Util.checkALError()
    player.ready.map { _ =>
      player
    }
  }

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

class ALStreamingPlayer(override val data: ALStreamingData, override val source: ALAbstractSource, override val alSource: Int, res: Resource) extends ALBasicPlayer(data, source, alSource) {

  private val converter: Converter = FixedSigned16Converter

  private[games] val streamingThread = {
    val streamingThread = new Thread() {
      override def run(): Unit = {
        // Init
        var decoder = new VorbisDecoder(JvmUtils.streamForResource(res), converter)

        val bufferedTime = 2.0f // amount of time buffered
        val numBuffers = 8 // buffers
        val streamingInterval = 1.0f // check for buffer every second

        require(streamingInterval < bufferedTime) // TODO remove later, sanity check, buffer will go empty before we can feed them else (beware of the pitch!)

        val bufferSampleSize = ((bufferedTime * decoder.rate) / numBuffers).toInt
        val bufferSize = bufferSampleSize * decoder.channels * converter.bytePerValue

        val tmpBufferData = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())

        val alBuffers = new Array[Int](numBuffers)

        val format = decoder.channels match {
          case 1 => AL10.AL_FORMAT_MONO16
          case 2 => AL10.AL_FORMAT_STEREO16
          case x => throw new RuntimeException("Only mono or stereo data are supported. Found channels: " + x)
        }

        for (i <- 0 until alBuffers.length) {
          alBuffers(i) = AL10.alGenBuffers()
        }
        Util.checkALError()

        var buffersReady: List[Int] = alBuffers.toList

        /**
         * Fill the buffer with the data from the decoder
         * Returns false if the end of the stream has been reached (but the data in the buffer are still valid up to the limit), true else
         */
        def fillBuffer(buffer: ByteBuffer): Boolean = {
          buffer.clear()

          val ret = try {
            decoder.readFully(buffer)
            true
          } catch {
            case e: EOFException => false
          }

          buffer.flip()

          ret
        }

        var running = true
        var last = System.currentTimeMillis()

        // Main thread loop
        while (threadRunning) {
          // if we are using this streaming thread...
          if (running) {
            // Retrieve the used buffer
            val processed = AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_PROCESSED)
            for (i <- 0 until processed) {
              val alBuffer = AL10.alSourceUnqueueBuffers(alSource)
              buffersReady = alBuffer :: buffersReady
            }

            // Fill the buffer and send them to OpenAL again
            while (running && !buffersReady.isEmpty) {
              val alBuffer = buffersReady.head
              buffersReady = buffersReady.tail

              running = fillBuffer(tmpBufferData)
              AL10.alBufferData(alBuffer, format, tmpBufferData, decoder.rate)
              AL10.alSourceQueueBuffers(alSource, alBuffer)
              Util.checkALError()

              // Check for looping
              if (!running && looping) {
                decoder.close()
                decoder = new VorbisDecoder(JvmUtils.streamForResource(res), converter)
                running = true
              }
            }

            // We should have enough data to start the playback at this point
            if (!promiseReady.isCompleted) promiseReady.success((): Unit)
          }

          // Sleep a while, adjust for pitch (playback rate)
          try {
            val now = System.currentTimeMillis()
            val elapsedTime = now - last
            last = System.currentTimeMillis()
            val remainingTime = streamingInterval - elapsedTime
            if (remainingTime > 0) { // Sleep only
              val sleepingTime = (remainingTime / pitchCache * 1000).toLong
              Thread.sleep(sleepingTime)
            }
          } catch {
            case e: InterruptedException => // just wake up and do your thing
          }

        }

        AL10.alSourceStop(alSource)

        // destroy the awaiting buffers
        buffersReady.foreach { alBuffer => AL10.alDeleteBuffers(alBuffer) }

        // destroy the buffers still in use
        val queuedBuffers = AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_QUEUED)
        for (i <- 0 until queuedBuffers) {
          val alBuffer = AL10.alSourceUnqueueBuffers(alSource)
          AL10.alDeleteBuffers(alBuffer)
        }

        AL10.alDeleteSources(alSource)

        Util.checkALError()

        // Closing
        decoder.close()
      }
    }
    streamingThread.setDaemon(true)
    streamingThread.start()

    streamingThread
  }

  private def wakeUpThread() {
    streamingThread.interrupt()
  }

  private var threadRunning = true
  private val promiseReady = Promise[Unit]
  private[games] val ready = promiseReady.future

  private var looping = false
  private var pitchCache = 1f

  def loop: Boolean = looping
  def loop_=(loop: Boolean): Unit = looping = loop

  def pitch: Float = {
    val ret = AL10.alGetSourcef(alSource, AL10.AL_PITCH)
    Util.checkALError()
    ret
  }
  def pitch_=(pitch: Float): Unit = {
    pitchCache = pitch
    AL10.alSourcef(alSource, AL10.AL_PITCH, pitch)
    Util.checkALError()
    wakeUpThread() // so it can adjust to the new playback rate
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

    threadRunning = false
    wakeUpThread()
  }
}
