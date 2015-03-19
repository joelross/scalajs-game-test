package games.audio

import scala.scalajs.js
import org.scalajs.dom
import games.Resource
import scala.concurrent.Promise
import games.JsUtils
import scala.concurrent.Future

import java.nio.{ ByteBuffer, ByteOrder, FloatBuffer }

import scalajs.concurrent.JSExecutionContext.Implicits.queue

class JsRawData private[games] (ctx: WebAudioContext, data: ByteBuffer, format: Format, channels: Int, freq: Int) extends RawData {
  private val bufferReady = Future {
    format match {
      case Format.FLOAT32 => // good to go
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

    var channelCur = 0
    while (channelCur < channels) {
      channelsData(channelCur) = buffer.getChannelData(channelCur).asInstanceOf[js.typedarray.Float32Array]
      channelCur += 1
    }

    var sampleCur = 0
    while (sampleCur < sampleCount) {
      channelCur = 0
      while (channelCur < channels) {
        channelsData(channelCur)(sampleCur) = floatBuffer.get()
        channelCur += 1
      }
      sampleCur += 1
    }

    buffer
  }

  def createSource: scala.concurrent.Future[games.audio.Source] = {
    bufferReady.map { buffer => new JsBufferedSource(ctx, buffer, ctx.webApi.destination) }
  }
  def createSource3D: scala.concurrent.Future[games.audio.Source3D] = {
    bufferReady.map { buffer =>
      val pannerNode = ctx.webApi.createPanner()
      val source2d = new JsBufferedSource(ctx, buffer, pannerNode)
      pannerNode.connect(ctx.webApi.destination)
      new JsSource3D(ctx, source2d, pannerNode)
    }
  }
}

class JsBufferedData private[games] (ctx: WebAudioContext, res: Resource) extends BufferedData {
  private val decodedDataReady = Promise[js.Dynamic]

  private val request = new dom.XMLHttpRequest()
  request.open("GET", JsUtils.pathForResource(res), true)
  request.responseType = "arraybuffer"

  request.onload = (e: dom.Event) => {
    val buffer = request.response.asInstanceOf[js.Dynamic]

    ctx.webApi.decodeAudioData(buffer,
      (decodedBuffer: js.Dynamic) => {
        decodedDataReady.success(decodedBuffer)
      },
      () => {
        decodedDataReady.failure(new RuntimeException("Failed to decode the audio data from resource " + res))
      })

  }
  request.onerror = (e: dom.Event) => {
    decodedDataReady.failure(new RuntimeException("Failed to retrieve resource " + res + ", cause: HTTP error " + request.status + " (" + request.responseText + ")"))
  }

  request.send()

  def createSource: Future[Source] = {
    decodedDataReady.future.map { buffer => new JsBufferedSource(ctx, buffer, ctx.webApi.destination) }
  }
  def createSource3D: Future[Source3D] = {
    decodedDataReady.future.map { buffer =>
      val pannerNode = ctx.webApi.createPanner()
      val source2d = new JsBufferedSource(ctx, buffer, pannerNode)
      pannerNode.connect(ctx.webApi.destination)
      new JsSource3D(ctx, source2d, pannerNode)
    }
  }
}

class JsStreamingData private[games] (ctx: WebAudioContext, res: Resource) extends StreamingData {
  private val streamReady = Promise[String]

  private val audio = js.Dynamic.newInstance(js.Dynamic.global.Audio)()
  private val path = JsUtils.pathForResource(res)
  audio.src = path

  audio.oncanplay = () => {
    streamReady.success(path)
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

    streamReady.failure(new RuntimeException("Failed to load the stream " + res + ", cause: " + errorMessage))
  }

  def createSource: Future[Source] = {
    val source = new JsStreamingSource(ctx, streamReady.future, ctx.webApi.destination)
    source.ready.map { x => source }
  }
  def createSource3D: Future[Source3D] = {
    val pannerNode = ctx.webApi.createPanner()
    val source = new JsStreamingSource(ctx, streamReady.future, pannerNode)
    source.ready.map { x =>
      pannerNode.connect(ctx.webApi.destination)
      new JsSource3D(ctx, source, pannerNode)
    }
  }
}
