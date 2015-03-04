package games.audio

import scala.scalajs.js
import org.scalajs.dom
import games.Resource
import scala.concurrent.Promise
import games.JsResourceUtil
import scala.concurrent.Future

import scalajs.concurrent.JSExecutionContext.Implicits.queue

class JsBufferedData private[games] (ctx: JsContext, res: Resource) extends BufferedData {
  private val decodedDataReady = Promise[js.typedarray.ArrayBuffer]

  private val request = new dom.XMLHttpRequest()
  request.open("GET", JsResourceUtil.pathForResource(res), true)
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
    val source = new JsBufferedSource(ctx, decodedDataReady.future)
    val promise = Promise[Source]

    source.ready.onSuccess { case _ => promise.success(source) }
    source.ready.onFailure { case t => promise.failure(t) }

    promise.future
  }
  def createSource3D: Future[Source3D] = ???
}

class JsStreamingData private[games] (ctx: JsContext, res: Resource) extends StreamingData {
  private val streamReady = Promise[String]

  private val audio = js.Dynamic.newInstance(js.Dynamic.global.Audio)()
  private val path = JsResourceUtil.pathForResource(res)
  audio.src = path

  audio.oncanplay = (e: dom.Event) => {
    streamReady.success(path)
  }

  audio.onerror = (e: dom.Event) => {
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
    val source = new JsStreamingSource(ctx, streamReady.future)
    val promise = Promise[Source]

    source.ready.onSuccess { case _ => promise.success(source) }
    source.ready.onFailure { case t => promise.failure(t) }

    promise.future
  }
  def createSource3D: Future[Source3D] = ???
}

class JsRawData private[games] (ctx: JsContext) extends RawData {
  // TODO manually filling buffer with a ByteBuffer

  def createSource: Future[Source] = ???
  def createSource3D: Future[Source3D] = ???
}