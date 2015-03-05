package games.audio

import scala.concurrent._
import org.lwjgl.openal.AL10
import org.lwjgl.openal.Util
import java.io.InputStream
import games.JvmResourceUtil
import games.Resource
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.EOFException

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
        // Init
        var decoder = new VorbisDecoder(JvmResourceUtil.streamForResource(res), converter)

        val bufferedTime = 2.0f // amount of time buffered
        val numBuffers = 8 // buffers
        val streamingInterval = 1.0f // check for buffer every second

        require(streamingInterval < bufferedTime) // sanity check, buffer will go empty before we can feed them else (beware of the pitch!)

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

        // Loop
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

            // Fill the buffer and enqueue them again
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
                decoder = new VorbisDecoder(JvmResourceUtil.streamForResource(res), converter)
                running = true
              }
            }

            if (!promiseReady.isCompleted) promiseReady.success((): Unit)
          }

          // Sleep a while, adjust for pitch (playback rate)
          val sleepingTime = (streamingInterval / pitchCache * 1000).toLong
          try {
            Thread.sleep(sleepingTime)
          } catch {
            case e: InterruptedException => // just wake up and do your thing
          }

        }

        AL10.alSourcePause(alSource)

        // destroy the awaiting buffers
        buffersReady.foreach { alBuffer => AL10.alDeleteBuffers(alBuffer) }

        // destroy the buffers still in use
        val queuedBuffers = AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_QUEUED)
        while (queuedBuffers > 0) {
          val alBuffer = AL10.alSourceUnqueueBuffers(alSource)
          AL10.alDeleteBuffers(alBuffer)
        }

        AL10.alDeleteSources(alSource)

        Util.checkALError()

        // Closing
        decoder.close()
      }
    })

    streamingThread.start()

    Util.checkALError()

    (alSource, streamingThread)
  }

  private val (alSource, streamingThread) = init()
  private var threadRunning = true

  private def wakeUpThread() {
    streamingThread.interrupt()
  }

  private var looping = false
  private var pitchCache = 1f

  def loop: Boolean = looping
  def loop_=(loop: Boolean): Unit = looping = loop
  def pause: Unit = AL10.alSourcePause(alSource)
  def play: Unit = AL10.alSourcePlay(alSource)
  def pitch: Float = AL10.alGetSourcef(alSource, AL10.AL_PITCH)
  def pitch_=(pitch: Float): Unit = {
    pitchCache = pitch
    AL10.alSourcef(alSource, AL10.AL_PITCH, pitch)
    wakeUpThread() // so it can adjust to the new playback rate
  }
  def volume: Float = AL10.alGetSourcef(alSource, AL10.AL_GAIN)
  def volume_=(volume: Float): Unit = AL10.alSourcef(alSource, AL10.AL_GAIN, volume)

  override def close(): Unit = {
    threadRunning = false
    wakeUpThread()
  }

  private val promiseReady = Promise[Unit]
  private[games] val ready = promiseReady.future
}

