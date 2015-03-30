package games.audio

import scala.concurrent._
import org.lwjgl.openal.AL10
import org.lwjgl.openal.Util
import java.io.InputStream
import games.JvmUtils
import games.Resource
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.EOFException

import games.math.Vector3f

trait ALSource extends AbstractSource {
  private[games] val alSource: Int
  private[games] def masterVolumeChanged(): Unit
}

class ALBufferedSource private[games] (ctx: ALContext, alBuffer: Int) extends Source with ALSource {

  private def init() = {
    val alSource = AL10.alGenSources()
    ctx.addSource(this)

    AL10.alSourcei(alSource, AL10.AL_BUFFER, alBuffer)

    Util.checkALError()
    alSource
  }

  private[games] val alSource = init()
  private[games] def masterVolumeChanged(): Unit = {
    updateVolume()
  }

  private var thisVolume = 1f
  this.updateVolume()

  private def updateVolume(): Unit = {
    val curVolume = ctx.masterVolume * thisVolume
    AL10.alSourcef(alSource, AL10.AL_GAIN, curVolume)
    Util.checkALError()
  }

  def loop: Boolean = {
    val ret = AL10.alGetSourcei(alSource, AL10.AL_LOOPING) == AL10.AL_TRUE
    Util.checkALError()
    ret
  }
  def loop_=(loop: Boolean): Unit = {
    AL10.alSourcei(alSource, AL10.AL_LOOPING, if (loop) AL10.AL_TRUE else AL10.AL_FALSE)
    Util.checkALError()
  }
  def pause: Unit = {
    AL10.alSourcePause(alSource)
    Util.checkALError()
  }
  def play: Unit = {
    AL10.alSourcePlay(alSource)
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
  def volume: Float = {
    //AL10.alGetSourcef(alSource, AL10.AL_GAIN)
    thisVolume
  }
  def volume_=(volume: Float): Unit = {
    thisVolume = volume
    updateVolume()
  }
  def playing: Boolean = {
    val ret = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING
    Util.checkALError()
    ret
  }

  override def close(): Unit = {
    super.close()
    ctx.removeSource(this)
    AL10.alDeleteSources(alSource)
    Util.checkALError()
  }
}

class ALStreamingSource private[games] (ctx: ALContext, res: Resource) extends Source with ALSource {

  private val converter: Converter = FixedSigned16Converter

  private def init() = {
    val alSource = AL10.alGenSources()
    ctx.addSource(this)
    Util.checkALError()

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
            var processed = AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_PROCESSED)
            while (processed > 0) {
              val alBuffer = AL10.alSourceUnqueueBuffers(alSource)
              buffersReady = alBuffer :: buffersReady
              processed -= 1
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
        var queuedBuffers = AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_QUEUED)
        while (queuedBuffers > 0) {
          val alBuffer = AL10.alSourceUnqueueBuffers(alSource)
          AL10.alDeleteBuffers(alBuffer)
          queuedBuffers -= 1
        }

        AL10.alDeleteSources(alSource)

        Util.checkALError()

        // Closing
        decoder.close()
      }
    }
    streamingThread.setDaemon(true)
    streamingThread.start()

    (alSource, streamingThread)
  }

  private[games] val (alSource, streamingThread) = init()
  private var threadRunning = true

  private def wakeUpThread() {
    streamingThread.interrupt()
  }

  private[games] def masterVolumeChanged(): Unit = {
    updateVolume()
  }

  private var thisVolume = 1f
  this.updateVolume()

  private def updateVolume(): Unit = {
    val curVolume = ctx.masterVolume * thisVolume
    AL10.alSourcef(alSource, AL10.AL_GAIN, curVolume)
    Util.checkALError()
  }

  private var looping = false
  private var pitchCache = 1f

  def loop: Boolean = looping
  def loop_=(loop: Boolean): Unit = looping = loop
  def pause: Unit = {
    AL10.alSourcePause(alSource)
    Util.checkALError()
  }
  def play: Unit = {
    AL10.alSourcePlay(alSource)
    Util.checkALError()
  }
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
  def volume: Float = {
    //AL10.alGetSourcef(alSource, AL10.AL_GAIN)
    thisVolume
  }
  def volume_=(volume: Float): Unit = {
    thisVolume = volume
    updateVolume()
  }
  def playing: Boolean = {
    val ret = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING
    Util.checkALError()
    ret
  }

  override def close(): Unit = {
    super.close()
    ctx.removeSource(this)
    threadRunning = false
    wakeUpThread()
  }

  private val promiseReady = Promise[Unit]
  private[games] val ready = promiseReady.future
}

class ALSource3D private[games] (ctx: ALContext, source: ALSource) extends Source3D with ALSource {
  private[games] val alSource = source.alSource

  private val positionBuffer = ByteBuffer.allocateDirect(3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

  // Preload buffer
  AL10.alGetSource(alSource, AL10.AL_POSITION, positionBuffer)
  Util.checkALError()

  def loop: Boolean = source.loop
  def loop_=(loop: Boolean): Unit = source.loop_=(loop)
  def pause: Unit = source.pause
  def pitch: Float = source.pitch
  def pitch_=(pitch: Float): Unit = source.pitch_=(pitch)
  def play: Unit = source.play
  def volume: Float = source.volume
  def volume_=(volume: Float): Unit = source.volume_=(volume)
  def playing: Boolean = source.playing

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
    AL10.alSource(alSource, AL10.AL_POSITION, positionBuffer)
    Util.checkALError()
  }

  def masterVolumeChanged(): Unit = {} // we are wrapping an ALBufferedSource or an ALStreamingSource that will already take care of that
}
