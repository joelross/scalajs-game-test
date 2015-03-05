package games.audio

import scala.scalajs.js
import org.scalajs.dom

import scala.concurrent.Future
import scala.concurrent.Promise
import scalajs.concurrent.JSExecutionContext.Implicits.queue

class JsBufferedSource private[games] (ctx: JsContext, buffer: js.typedarray.ArrayBuffer) extends Source {
  private val sourceNode = ctx.webApi.createBufferSource()
  sourceNode.buffer = buffer
  sourceNode.loop = false

  private val gainNode = ctx.webApi.createGain()
  sourceNode.connect(gainNode)

  gainNode.gain.value = 1.0

  gainNode.connect(ctx.webApi.destination)

  def pause: Unit = {
    sourceNode.stop() // TODO check position for start & pause (beginning or where it was left?)
  }

  def play: Unit = {
    sourceNode.start()
  }

  def volume: Float = gainNode.gain.value.asInstanceOf[Double].toFloat

  def volume_=(volume: Float): Unit = {
    gainNode.gain.value = volume.toDouble
  }

  def loop: Boolean = sourceNode.loop.asInstanceOf[Boolean]

  def loop_=(loop: Boolean): Unit = {
    sourceNode.loop = loop
  }

  def pitch: Float = sourceNode.playbackRate.value.asInstanceOf[Double].toFloat

  def pitch_=(pitch: Float): Unit = {
    sourceNode.playbackRate.value = pitch.toDouble
  }
}

class JsStreamingSource private[games] (ctx: JsContext, pathFuture: Future[String]) extends Source {
  private val promiseReady = Promise[Unit]

  private val audio = js.Dynamic.newInstance(js.Dynamic.global.Audio)()
  private val sourceNode = ctx.webApi.createMediaElementSource(audio)
  sourceNode.connect(ctx.webApi.destination)

  pathFuture.onSuccess {
    case path =>
      audio.src = path
  }
  pathFuture.onFailure {
    case t =>
      promiseReady.failure(t)
  }

  audio.oncanplay = (e: dom.Event) => {
    promiseReady.success((): Unit)
  }

  audio.onerror = (e: dom.Event) => {
    val msg = "Failure of streaming"

    if (!promiseReady.isCompleted) promiseReady.failure(new RuntimeException(msg))
    else println(msg)
  }

  def loop: Boolean = audio.loop.asInstanceOf[Boolean]

  def loop_=(loop: Boolean): Unit = {
    audio.loop = loop
  }

  def pause: Unit = {
    audio.pause()
  }

  def play: Unit = {
    audio.play()
  }

  def pitch: Float = audio.playbackRate.asInstanceOf[Double].toFloat

  def pitch_=(pitch: Float): Unit = {
    audio.playbackRate = pitch.toDouble
  }

  def volume: Float = audio.volume.asInstanceOf[Double].toFloat

  def volume_=(volume: Float): Unit = {
    audio.volume = volume.toDouble
  }

  private[games] val ready = promiseReady.future
}
