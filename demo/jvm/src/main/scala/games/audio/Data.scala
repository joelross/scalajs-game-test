package games.audio

import games.JvmResourceUtil
import games.Resource
import org.lwjgl.openal.AL10
import org.lwjgl.openal.Util
import games.ALUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.ByteArrayOutputStream
import java.io.EOFException

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

class ALBufferedData private[games] (ctx: ALContext, res: Resource) extends BufferedData {
  private val bufferReady = Future {
    val alBuffer = AL10.alGenBuffers()

    val in = JvmResourceUtil.streamForResource(res)

    val decoder = new VorbisDecoder(in, new FixedSigned16Converter)

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
    require(totalDataLength == dataArray.length) // sanity check
    val dataBuffer = ByteBuffer.allocateDirect(dataArray.length).order(ByteOrder.nativeOrder())

    dataBuffer.put(dataArray)
    dataBuffer.rewind()

    val format = decoder.channels match {
      case 1 => AL10.AL_FORMAT_MONO16
      case 2 => AL10.AL_FORMAT_STEREO16
      case x => throw new RuntimeException("Only mono or stereo data are supported. Found channels: " + x)
    }

    AL10.alBufferData(alBuffer, format, dataBuffer, decoder.rate)

    Util.checkALError()

    alBuffer
  }

  def createSource: scala.concurrent.Future[games.audio.Source] = bufferReady.map { case alBuffer => new ALBufferedSource(ctx, alBuffer) }
  def createSource3D: scala.concurrent.Future[games.audio.Source3D] = ???
}

class ALStreamingData private[games] (ctx: ALContext, res: Resource) extends StreamingData {
  def createSource: scala.concurrent.Future[games.audio.Source] = ???
  def createSource3D: scala.concurrent.Future[games.audio.Source3D] = ???
}