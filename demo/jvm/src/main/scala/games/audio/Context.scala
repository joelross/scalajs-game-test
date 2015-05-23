package games.audio

import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10
import org.lwjgl.openal.Util

import games.math.Vector3f
import games.JvmUtils

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.ByteArrayOutputStream
import java.io.EOFException

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.Set

class ALContext extends Context {
  AL.create()

  def prepareBufferedData(res: games.Resource): scala.concurrent.Future[games.audio.BufferedData] = Future {
    val alBuffer = AL10.alGenBuffers()
    val in = JvmUtils.streamForResource(res)
    val decoder = new VorbisDecoder(in, FixedSigned16Converter)
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

    val ret = new ALBufferData(this, alBuffer)
    Util.checkALError()
    ret
  }
  def prepareRawData(data: java.nio.ByteBuffer, format: games.audio.Format, channels: Int, freq: Int): scala.concurrent.Future[games.audio.BufferedData] = Future {
    val alBuffer = AL10.alGenBuffers()

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
    Util.checkALError()
    ret
  }
  def prepareStreamingData(res: games.Resource): scala.concurrent.Future[games.audio.Data] = Future.successful(new ALStreamingData(this, res))

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
    AL.destroy()
  }
}

class ALListener private[games] () extends Listener {
  private val orientationBuffer = ByteBuffer.allocateDirect(2 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
  private val positionBuffer = ByteBuffer.allocateDirect(1 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

  // Preload buffer
  AL10.alGetListener(AL10.AL_POSITION, positionBuffer)
  AL10.alGetListener(AL10.AL_ORIENTATION, orientationBuffer)
  Util.checkALError()

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
    AL10.alListener(AL10.AL_POSITION, positionBuffer)
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
    AL10.alListener(AL10.AL_ORIENTATION, orientationBuffer)
  }
}