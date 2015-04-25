package games.audio

import scala.scalajs.js
import org.scalajs.dom
import games.Resource
import scala.concurrent.Promise
import games.{ Utils, JsUtils }
import scala.concurrent.Future

import java.nio.{ ByteBuffer, ByteOrder, FloatBuffer }

import scalajs.concurrent.JSExecutionContext.Implicits.queue

private[games] trait DataJS {
  private[games] def createSource(outputNode: js.Dynamic): scala.concurrent.Future[games.audio.Source]
}

private[games] object Helper {
  def createDataFromAurora(ctx: WebAudioContext, res: Resource): scala.concurrent.Future[DataJS] = {
    ???
  }

  def createSource3D(ctx: WebAudioContext, data: DataJS): scala.concurrent.Future[games.audio.Source3D] = {
    val pannerNode = ctx.webApi.createPanner()
    data.createSource(pannerNode).map { source2d =>
      pannerNode.connect(ctx.mainOutput)
      new JsSource3D(ctx, source2d, pannerNode)
    }
  }
}

class JsRawData private[games] (ctx: WebAudioContext, data: ByteBuffer, format: Format, channels: Int, freq: Int) extends games.audio.Data with DataJS {
  private val bufferReady = Future {
    format match {
      case Format.Float32 => // good to go
      case _              => throw new RuntimeException("Unsupported data format: " + format)
    }

    channels match {
      case 1 => // good to go
      case 2 => // good to go
      case _ => throw new RuntimeException("Unsupported channels number: " + channels)
    }

    val floatBuffer = data.slice().order(ByteOrder.nativeOrder()).asFloatBuffer()

    val sampleCount = floatBuffer.remaining() / channels

    val buffer = ctx.webApi.createBuffer(channels, sampleCount, freq)

    var channelsData = new Array[js.typedarray.Float32Array](channels)

    for (channelCur <- 0 until channels) {
      channelsData(channelCur) = buffer.getChannelData(channelCur).asInstanceOf[js.typedarray.Float32Array]
    }

    for (sampleCur <- 0 until sampleCount) {
      for (channelCur <- 0 until channels) {
        channelsData(channelCur)(sampleCur) = floatBuffer.get()
      }
    }

    buffer
  }

  private[games] def createSource(outputNode: js.Dynamic): scala.concurrent.Future[games.audio.Source] = {
    bufferReady.map { buffer => new JsBufferedSource(ctx, buffer, outputNode) }
  }

  def createSource(): scala.concurrent.Future[games.audio.Source] = this.createSource(ctx.mainOutput)
  def createSource3D(): scala.concurrent.Future[games.audio.Source3D] = Helper.createSource3D(ctx, this)
}

class JsBufferedData private[games] (ctx: WebAudioContext, res: Resource) extends games.audio.Data with DataJS {
  // Init
  private val decodedDataReady = {
    val dataFuture = Utils.getBinaryDataFromResource(res)
    val promise = Promise[js.Dynamic]

    dataFuture.map { bb =>
      import scala.scalajs.js.typedarray.TypedArrayBufferOps._

      val arrayBuffer = bb.arrayBuffer()
      ctx.webApi.decodeAudioData(arrayBuffer,
        (decodedBuffer: js.Dynamic) => {
          promise.success(decodedBuffer)
        },
        () => {
          promise.failure(new RuntimeException("Failed to decode the audio data from resource " + res))
        })
    }

    promise.future
  }

  private[games] def createSource(outputNode: js.Dynamic): Future[games.audio.Source] = {
    decodedDataReady.map { buffer => new JsBufferedSource(ctx, buffer, outputNode) }
  }

  def createSource(): Future[games.audio.Source] = this.createSource(ctx.mainOutput)
  def createSource3D(): Future[games.audio.Source3D] = Helper.createSource3D(ctx, this)
}

class JsStreamingData private[games] (ctx: WebAudioContext, res: Resource) extends games.audio.Data with DataJS {
  private[games] def createSource(outputNode: js.Dynamic): Future[games.audio.Source] = {
    val path = JsUtils.pathForResource(res)
    val promise = Promise[games.audio.Source]
    val audio: js.Dynamic = js.Dynamic.newInstance(js.Dynamic.global.Audio)()
    audio.src = path

    audio.oncanplay = () => {
      val source = new JsStreamingSource(ctx, audio, outputNode)
      promise.success(source)
    }

    audio.onerror = () => {
      val errorCode = audio.error.code.asInstanceOf[Int]
      val errorMessage = errorCode match {
        case 1 => "request aborted"
        case 2 => "network error"
        case 3 => "decoding error"
        case 4 => "source not supported"
        case _ => "unknown error"
      }
      val msg = "Failed to load the stream " + res + ", cause: " + errorMessage

      // If Aurora is available and this error seems due to decoding, try with Aurora
      if (WebAudioContext.canUseAurora && (errorCode == 3 || errorCode == 4)) {
        val dataFuture = Helper.createDataFromAurora(ctx, res).flatMap { data => data.createSource(outputNode) }
        dataFuture.onSuccess { case source => promise.success(source) }
        dataFuture.onFailure { case t => promise.failure(new RuntimeException(msg + " (result with Aurora: " + t + ")", t)) }
      } else {
        if (!promise.isCompleted) promise.failure(new RuntimeException(msg))
        else Console.err.println(msg)
      }
    }

    promise.future
  }

  def createSource(): Future[games.audio.Source] = this.createSource(ctx.mainOutput)
  def createSource3D(): Future[games.audio.Source3D] = Helper.createSource3D(ctx, this)
}
