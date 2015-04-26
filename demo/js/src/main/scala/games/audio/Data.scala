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
  def createDataFromAurora(ctx: WebAudioContext, arraybuffer: js.typedarray.ArrayBuffer): scala.concurrent.Future[DataJS] = {
    val promise = Promise[DataJS]

    val asset = js.Dynamic.global.AV.Asset.fromBuffer(arraybuffer)
    asset.on("error", (error: String) => {
      promise.failure(new RuntimeException("Aurora returned error: " + error))
    })

    asset.decodeToBuffer((data: js.typedarray.Float32Array) => {
      val arraybuffer = data.buffer
      val byteBuffer = js.typedarray.TypedArrayBuffer.wrap(arraybuffer)

      var optFormat: Option[js.Dynamic] = None
      asset.get("format", (format: js.Dynamic) => {
        optFormat = Some(format)
      })

      optFormat match {
        case Some(format) =>
          val channels = format.channelsPerFrame.asInstanceOf[Int]
          val sampleRate = format.sampleRate.asInstanceOf[Int]
          val rawData = new JsRawData(ctx, byteBuffer, Format.Float32, channels, sampleRate)
          promise.success(rawData)

        case None =>
          promise.failure(new RuntimeException("Decoding done, but failed to retrieve the format from Aurora"))
      }
    })

    promise.future
  }

  def createDataFromAurora(ctx: WebAudioContext, res: Resource): scala.concurrent.Future[DataJS] = {
    Utils.getBinaryDataFromResource(res).flatMap { bb =>
      import scala.scalajs.js.typedarray.TypedArrayBufferOps._

      val arrayBuffer = bb.arrayBuffer()
      this.createDataFromAurora(ctx, arrayBuffer)
    }
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
  private val bufferReady = Future { // Don't do it right now, it would block everything
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
    val promise = Promise[Either[js.Dynamic, DataJS]]

    dataFuture.map { bb =>
      import scala.scalajs.js.typedarray.TypedArrayBufferOps._

      val arraybuffer = bb.arrayBuffer()
      ctx.webApi.decodeAudioData(arraybuffer,
        (decodedBuffer: js.Dynamic) => {
          promise.success(Left(decodedBuffer))
        },
        () => {
          val msg = "Failed to decode the audio data from resource " + res
          // If Aurora is available and this error seems due to decoding, try with Aurora
          if (WebAudioContext.canUseAurora) {
            val auroraDataFuture = Helper.createDataFromAurora(ctx, arraybuffer)
            auroraDataFuture.onSuccess { case auroraData => promise.success(Right(auroraData)) }
            auroraDataFuture.onFailure { case t => promise.failure(new RuntimeException(msg + " (result with Aurora: " + t + ")", t)) }
          } else {
            promise.failure(new RuntimeException(msg))
          }
        })
    }

    promise.future
  }

  private[games] def createSource(outputNode: js.Dynamic): Future[games.audio.Source] = {
    decodedDataReady.flatMap {
      case Left(buffer)      => Future.successful(new JsBufferedSource(ctx, buffer, outputNode))
      case Right(auroraData) => auroraData.createSource(outputNode)
    }
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
        val auroraDataFuture = Helper.createDataFromAurora(ctx, res).flatMap { data => data.createSource(outputNode) }
        auroraDataFuture.onSuccess { case source => promise.success(source) }
        auroraDataFuture.onFailure { case t => promise.failure(new RuntimeException(msg + " (result with Aurora: " + t + ")", t)) }
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
