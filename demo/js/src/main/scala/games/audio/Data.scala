package games.audio

import scala.scalajs.js
import org.scalajs.dom
import games.Resource
import scala.concurrent.Promise
import games.JsUtils
import scala.concurrent.Future

import scalajs.concurrent.JSExecutionContext.Implicits.queue

class JsBufferedData private[games] (ctx: WebAudioContext, res: Resource) extends BufferedData {
  private val decodedDataReady = Promise[js.typedarray.ArrayBuffer]

  private val request = new dom.XMLHttpRequest()
  request.open("GET", JsUtils.pathForResource(res), true)
  request.responseType = "arraybuffer"

  request.onload = (e: dom.Event) => {
    val buffer = request.response.asInstanceOf[js.typedarray.ArrayBuffer]

    ctx.webApi.decodeAudioData(buffer,
      (decodedArrayBuffer: js.typedarray.ArrayBuffer) => {
        decodedDataReady.success(decodedArrayBuffer)
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
    decodedDataReady.future.map { arrayBuffer => new JsBufferedSource(ctx, arrayBuffer, ctx.webApi.destination) }
  }
  def createSource3D: Future[Source3D] = {
    decodedDataReady.future.map { arrayBuffer =>
      val pannerNode = ctx.webApi.createPanner()
      val source2d = new JsBufferedSource(ctx, arrayBuffer, pannerNode)
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
