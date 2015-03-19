package games.audio

import games.JvmUtils
import games.Resource
import org.lwjgl.openal.AL10
import org.lwjgl.openal.Util
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.ByteArrayOutputStream
import java.io.EOFException

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

class ALRawData private[games] (ctx: ALContext, data: ByteBuffer, format: Format, channels: Int, freq: Int) extends RawData {
  private val bufferReady = Future {
    format match {
      case Format.FLOAT32 => // good to go
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

    val alBuffer = AL10.alGenBuffers()

    var sampleCur = 0
    while (sampleCur < sampleCount) {
      var channelCur = 0
      while (channelCur < channels) {
        val value = fb.get()
        converter(value, openalData)
        channelCur += 1
      }

      sampleCur += 1
    }

    openalData.rewind()

    AL10.alBufferData(alBuffer, channelFormat, openalData, freq)

    Util.checkALError()

    alBuffer
  }

  def createSource: scala.concurrent.Future[games.audio.Source] = {
    bufferReady.map { alBuffer => new ALBufferedSource(ctx, alBuffer) }
  }
  def createSource3D: scala.concurrent.Future[games.audio.Source3D] = {
    bufferReady.map { alBuffer => new ALSource3D(ctx, new ALBufferedSource(ctx, alBuffer)) }
  }

  override def close(): Unit = {
    bufferReady.onSuccess {
      case alBuffer =>
        AL10.alDeleteBuffers(alBuffer)
        Util.checkALError()
    }
  }
}

class ALBufferedData private[games] (ctx: ALContext, res: Resource) extends BufferedData {
  private val bufferReady = Future {
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

    Util.checkALError()

    alBuffer
  }

  def createSource: scala.concurrent.Future[games.audio.Source] = {
    bufferReady.map { alBuffer => new ALBufferedSource(ctx, alBuffer) }
  }
  def createSource3D: scala.concurrent.Future[games.audio.Source3D] = {
    bufferReady.map { alBuffer => new ALSource3D(ctx, new ALBufferedSource(ctx, alBuffer)) }
  }

  override def close(): Unit = {
    bufferReady.onSuccess {
      case alBuffer =>
        AL10.alDeleteBuffers(alBuffer)
        Util.checkALError()
    }
  }
}

class ALStreamingData private[games] (ctx: ALContext, res: Resource) extends StreamingData {
  def createSource: scala.concurrent.Future[games.audio.Source] = {
    val source = new ALStreamingSource(ctx, res)
    source.ready.map { x => source }
  }
  def createSource3D: scala.concurrent.Future[games.audio.Source3D] = {
    val source2d = new ALStreamingSource(ctx, res)
    source2d.ready.map { source => new ALSource3D(ctx, source2d) }
  }
}