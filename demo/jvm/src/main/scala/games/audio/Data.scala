package games.audio

import games.Resource
import games.math.Vector3f
import games.JvmUtils

import scala.concurrent.{ Promise, Future }
import scala.concurrent.ExecutionContext.Implicits.global

import scala.collection.{ mutable, immutable }

import java.nio.{ ByteBuffer, ByteOrder }
import java.io.EOFException

import org.lwjgl.openal
import org.lwjgl.openal.AL10

sealed trait ALAbstractSource extends Source {
  override def close(): Unit = {
    super.close()
  }
}

class ALSource(val ctx: ALContext) extends Source with ALAbstractSource {
  ctx.registerSource(this)

  override private[games] def registerPlayer(player: Player): Unit = {
    super.registerPlayer(player)

    // Apply spatial attributes right now
    val alSource = player.asInstanceOf[ALPlayer].alSource
    AL10.alSourcei(alSource, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE)
    AL10.alSource3f(alSource, AL10.AL_POSITION, 0f, 0f, 0f)
    AL10.alSource3f(alSource, AL10.AL_VELOCITY, 0f, 0f, 0f)

    openal.ALUtil.checkALError()
  }

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
      AL10.alSourcefv(alSource, AL10.AL_POSITION, positionBuffer)
    }
    openal.ALUtil.checkALError()
  }

  override private[games] def registerPlayer(player: Player): Unit = {
    super.registerPlayer(player)

    // Apply spatial attributes right now
    val alSource = player.asInstanceOf[ALPlayer].alSource
    AL10.alSourcefv(alSource, AL10.AL_POSITION, positionBuffer)
    AL10.alGetSourcefv(alSource, AL10.AL_POSITION, positionBuffer)
    openal.ALUtil.checkALError()
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

  def attachNow(source: games.audio.Source): games.audio.Player = {
    val alSource = AL10.alGenSources()
    try {
      AL10.alSourcei(alSource, AL10.AL_BUFFER, alBuffer)

      val alAudioSource = source.asInstanceOf[ALAbstractSource]
      val ret = new ALBufferPlayer(this, alAudioSource, alSource)
      openal.ALUtil.checkALError()
      ret
    } catch {
      case t: Throwable =>
        AL10.alDeleteSources(alSource)
        openal.ALUtil.checkALError()
        throw t
    }
  }

  override def close(): Unit = {
    super.close()

    ctx.unregisterData(this)

    AL10.alDeleteBuffers(alBuffer)
    openal.ALUtil.checkALError()
  }
}

class ALStreamingData(val ctx: ALContext, res: Resource) extends Data with ALData {
  ctx.registerData(this)

  def attach(source: games.audio.Source): scala.concurrent.Future[games.audio.Player] = {
    val promise = Promise[games.audio.Player]

    val alSource = AL10.alGenSources()
    val alAudioSource = source.asInstanceOf[ALAbstractSource]
    val player = new ALStreamingPlayer(this, alAudioSource, alSource, res)
    openal.ALUtil.checkALError()

    player.ready.onSuccess { case _ => promise.success(player) }
    player.ready.onFailure {
      case t: Throwable =>
        promise.failure(t)
        openal.ALUtil.checkALError()
    }

    promise.future
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
    openal.ALUtil.checkALError()
  }

  // Init
  applyChangedVolume()

  def volume: Float = thisVolume
  def volume_=(volume: Float): Unit = {
    thisVolume = volume
    applyChangedVolume
  }

  override def close(): Unit = {
    super.close()

    source.unregisterPlayer(this)
    data.unregisterPlayer(this)
  }
}

class ALBufferPlayer(override val data: ALBufferData, override val source: ALAbstractSource, override val alSource: Int) extends ALBasicPlayer(data, source, alSource) {
  def loop: Boolean = {
    val ret = AL10.alGetSourcei(alSource, AL10.AL_LOOPING) == AL10.AL_TRUE
    openal.ALUtil.checkALError()
    ret
  }
  def loop_=(loop: Boolean): Unit = {
    AL10.alSourcei(alSource, AL10.AL_LOOPING, if (loop) AL10.AL_TRUE else AL10.AL_FALSE)
    openal.ALUtil.checkALError()
  }

  def pitch: Float = {
    val ret = AL10.alGetSourcef(alSource, AL10.AL_PITCH)
    openal.ALUtil.checkALError()
    ret
  }
  def pitch_=(pitch: Float): Unit = {
    AL10.alSourcef(alSource, AL10.AL_PITCH, pitch)
    openal.ALUtil.checkALError()
  }

  def playing: Boolean = {
    val ret = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING
    openal.ALUtil.checkALError()
    ret
  }
  def playing_=(playing: Boolean): Unit = if (playing) {
    AL10.alSourcePlay(alSource)
    openal.ALUtil.checkALError()
  } else {
    AL10.alSourcePause(alSource)
    openal.ALUtil.checkALError()
  }

  override def close(): Unit = {
    super.close()

    AL10.alDeleteSources(alSource)
    openal.ALUtil.checkALError()
  }
}

class ALStreamingPlayer(override val data: ALStreamingData, override val source: ALAbstractSource, override val alSource: Int, res: Resource) extends ALBasicPlayer(data, source, alSource) { thisPlayer =>

  private val converter: Converter = FixedSigned16Converter

  private def initStreamingThread() = {
    val streamingThread = new Thread() { thisStreamingThread =>
      override def run(): Unit = {
        data.ctx.registerStreamingThread(thisStreamingThread)
        val numBuffers = 8 // buffers
        val alBuffers = new Array[Int](numBuffers)
        var decoder: VorbisDecoder = null
        try {
          // Init
          decoder = new VorbisDecoder(JvmUtils.streamForResource(res), converter)

          val bufferedTime = 2.0f // amount of time buffered

          val streamingInterval = 1.0f // check for buffer every second

          require(streamingInterval < bufferedTime) // TODO remove later, sanity check, buffer will go empty before we can feed them else (beware of the pitch!)

          val bufferSampleSize = ((bufferedTime * decoder.rate) / numBuffers).toInt
          val bufferSize = bufferSampleSize * decoder.channels * converter.bytePerValue

          val tmpBufferData = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())

          val format = decoder.channels match {
            case 1 => AL10.AL_FORMAT_MONO16
            case 2 => AL10.AL_FORMAT_STEREO16
            case x => throw new RuntimeException("Only mono or stereo data are supported. Found channels: " + x)
          }

          for (i <- 0 until alBuffers.length) {
            alBuffers(i) = AL10.alGenBuffers()
          }
          openal.ALUtil.checkALError()

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
                openal.ALUtil.checkALError()

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

          // Closing
          decoder.close()
          decoder = null
        } catch {
          case t: Throwable =>
            val ex = new RuntimeException("Error in the streaming thread", t)
            if (promiseReady.isCompleted) throw ex
            else promiseReady.failure(ex)
        } finally {
          if (decoder != null) {
            decoder.close()
            decoder = null
          }
          AL10.alDeleteSources(alSource)
          for (alBuffer <- alBuffers) {
            AL10.alDeleteBuffers(alBuffer)
          }
          data.ctx.unregisterStreamingThread(thisStreamingThread)
          openal.ALUtil.checkALError()
        }
      }
    }
    streamingThread.setDaemon(true)
    streamingThread.start()

    streamingThread
  }

  private[games] var streamingThread = this.initStreamingThread()

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
    val ret = if (AL10.alIsSource(alSource)) AL10.alGetSourcef(alSource, AL10.AL_PITCH) else pitchCache
    openal.ALUtil.checkALError()
    ret
  }
  def pitch_=(pitch: Float): Unit = {
    pitchCache = pitch
    if (AL10.alIsSource(alSource)) AL10.alSourcef(alSource, AL10.AL_PITCH, pitch)
    openal.ALUtil.checkALError()
    wakeUpThread() // so it can adjust to the new playback rate
  }

  def playing: Boolean = {
    val ret = if (AL10.alIsSource(alSource)) AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING else false
    openal.ALUtil.checkALError()
    ret
  }
  def playing_=(playing: Boolean): Unit = if (playing) {
    if (AL10.alIsSource(alSource)) AL10.alSourcePlay(alSource)
    openal.ALUtil.checkALError()
  } else {
    if (AL10.alIsSource(alSource)) AL10.alSourcePause(alSource)
    openal.ALUtil.checkALError()
  }

  override def close(): Unit = {
    super.close()

    threadRunning = false
    wakeUpThread()
  }
}
