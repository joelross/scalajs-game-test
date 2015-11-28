package games.audio

import games.math.Vector3f
import games.JvmUtils

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.ByteArrayOutputStream
import java.io.EOFException

import scala.concurrent.{ Promise, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.{ mutable, immutable }

import org.lwjgl.openal
import org.lwjgl.openal.AL10

class ALContext extends Context {
  private val streamingThreads: mutable.Set[Thread] = mutable.Set()
  private val lock = new Object()
  private[games] def registerStreamingThread(thread: Thread): Unit = lock.synchronized { streamingThreads += thread }
  private[games] def unregisterStreamingThread(thread: Thread): Unit = lock.synchronized {
    streamingThreads -= thread
    lock.notifyAll()
  }
  private[games] def waitForStreamingThreads(): Unit = lock.synchronized {
    while (!streamingThreads.isEmpty) lock.wait()
  }

  private lazy val fakeSource = this.createSource()

  // Init
  private val (alDevice, alContext) = {
    val alDevice = openal.ALDevice.create()
    val alContext = openal.ALContext.create(alDevice)
    
    openal.ALUtil.checkALError()
    
    (alDevice, alContext)
  }

  def prepareBufferedData(res: games.Resource): scala.concurrent.Future[games.audio.BufferedData] = Future {
    val alBuffer = AL10.alGenBuffers()
    var decoder: VorbisDecoder = null
    try {
      val in = JvmUtils.streamForResource(res)
      decoder = new VorbisDecoder(in, FixedSigned16Converter)
      val transfertBufferSize = 4096
      val transfertBuffer = ByteBuffer.allocate(transfertBufferSize).order(ByteOrder.nativeOrder())
      val dataStream = new ByteArrayOutputStream(transfertBufferSize)

      var totalDataLength = 0

      try {
        while (true) {
          transfertBuffer.rewind()
          val dataLength = decoder.read(transfertBuffer)
          val data = transfertBuffer.array()
          dataStream.write(data, 0, dataLength)
          totalDataLength += dataLength
        }
      } catch {
        case e: EOFException => // end of stream reached, exit loop
      }

      val dataArray = dataStream.toByteArray()
      require(totalDataLength == dataArray.length) // TODO remove later, sanity check
      val dataBuffer = ByteBuffer.allocateDirect(dataArray.length).order(ByteOrder.nativeOrder())

      dataBuffer.put(dataArray)
      dataBuffer.rewind()

      val format = decoder.channels match {
        case 1 => AL10.AL_FORMAT_MONO16
        case 2 => AL10.AL_FORMAT_STEREO16
        case x => throw new RuntimeException("Only mono or stereo data are supported. Found channels: " + x)
      }

      AL10.alBufferData(alBuffer, format, dataBuffer, decoder.rate)

      decoder.close()
      decoder = null

      val ret = new ALBufferData(this, alBuffer)
      openal.ALUtil.checkALError()
      ret
    } catch {
      case t: Throwable =>
        if (decoder != null) {
          decoder.close()
          decoder = null
        }
        AL10.alDeleteBuffers(alBuffer)
        openal.ALUtil.checkALError()
        throw t
    }
  }
  def prepareRawData(data: java.nio.ByteBuffer, format: games.audio.Format, channels: Int, freq: Int): scala.concurrent.Future[games.audio.BufferedData] = Future {
    val alBuffer = AL10.alGenBuffers()
    try {
      format match {
        case Format.Float32 => // good to go
        case _              => throw new RuntimeException("Unsupported data format: " + format)
      }

      val channelFormat = channels match {
        case 1 => AL10.AL_FORMAT_MONO16
        case 2 => AL10.AL_FORMAT_STEREO16
        case _ => throw new RuntimeException("Unsupported channels number: " + channels)
      }

      val converter = FixedSigned16Converter
      val fb = data.slice().order(ByteOrder.nativeOrder()).asFloatBuffer()

      val sampleCount = fb.remaining() / channels

      val openalData = ByteBuffer.allocateDirect(2 * channels * sampleCount).order(ByteOrder.nativeOrder())

      for (sampleCur <- 0 until sampleCount) {
        for (channelCur <- 0 until channels) {
          val value = fb.get()
          converter(value, openalData)
        }
      }

      openalData.rewind()

      AL10.alBufferData(alBuffer, channelFormat, openalData, freq)

      val ret = new ALBufferData(this, alBuffer)
      openal.ALUtil.checkALError()
      ret
    } catch {
      case t: Throwable =>
        AL10.alDeleteBuffers(alBuffer)
        openal.ALUtil.checkALError()
        throw t
    }
  }
  def prepareStreamingData(res: games.Resource): scala.concurrent.Future[games.audio.Data] = {
    val promise = Promise[games.audio.Data]

    val data = new ALStreamingData(this, res)

    // Try to create a player (to make sure it works)
    val playerFuture = data.attach(fakeSource)
    playerFuture.onSuccess {
      case player =>
        player.close()
        promise.success(data)
    }
    playerFuture.onFailure {
      case t =>
        data.close()
        promise.failure(t)
    }

    promise.future
  }

  def createSource(): games.audio.Source = new ALSource(this)
  def createSource3D(): games.audio.Source3D = new ALSource3D(this)

  val listener: games.audio.Listener = new ALListener()

  def volume: Float = masterVolume
  def volume_=(volume: Float): Unit = {
    masterVolume = volume
    for (
      source <- sources;
      player <- source.players
    ) {
      val alPlayer = player.asInstanceOf[ALPlayer]
      alPlayer.applyChangedVolume()
    }
  }

  private[games] var masterVolume = 1f

  override def close(): Unit = {
    super.close()

    // Wait for all the streaming threads to have done their work
    this.waitForStreamingThreads()

    alContext.destroy()
    alDevice.destroy()
    
    openal.ALUtil.checkALError()
  }
}

class ALListener private[games] () extends Listener {
  private val orientationBuffer = ByteBuffer.allocateDirect(2 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
  private val positionBuffer = ByteBuffer.allocateDirect(1 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

  // Preload buffer
  AL10.alGetListenerfv(AL10.AL_POSITION, positionBuffer)
  AL10.alGetListenerfv(AL10.AL_ORIENTATION, orientationBuffer)
  openal.ALUtil.checkALError()

  def position: Vector3f = {
    positionBuffer.rewind()
    val ret = new Vector3f
    ret.load(positionBuffer)
    ret
  }
  def position_=(position: Vector3f): Unit = {
    positionBuffer.rewind()
    position.store(positionBuffer)
    positionBuffer.rewind()
    AL10.alListenerfv(AL10.AL_POSITION, positionBuffer)
  }

  def up: Vector3f = {
    orientationBuffer.position(3)
    val ret = new Vector3f
    ret.load(orientationBuffer)
    ret
  }

  def orientation: Vector3f = {
    orientationBuffer.rewind()
    val ret = new Vector3f
    ret.load(orientationBuffer)
    ret
  }
  def setOrientation(orientation: Vector3f, up: Vector3f): Unit = {
    orientationBuffer.rewind()
    orientation.store(orientationBuffer)
    up.store(orientationBuffer)
    orientationBuffer.rewind()
    AL10.alListenerfv(AL10.AL_ORIENTATION, orientationBuffer)
  }
}
