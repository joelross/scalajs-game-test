package games.audio

import scala.scalajs.js
import org.scalajs.dom

import scala.concurrent.Future
import scala.concurrent.Promise
import scalajs.concurrent.JSExecutionContext.Implicits.queue

import games.math.Vector3f

class JsBufferedSource private[games] (ctx: WebAudioContext, buffer: js.Dynamic, outputNode: js.Dynamic) extends Source {
  private var sourceNode = ctx.webApi.createBufferSource()
  sourceNode.buffer = buffer

  private val gainNode = ctx.webApi.createGain()
  sourceNode.connect(gainNode)

  gainNode.gain.value = 1.0

  gainNode.connect(outputNode)

  private var isPlaying = false

  private var needRestarting = false
  private var nextStartTime = 0.0
  private var lastStartDate = 0.0

  private def now(): Double = js.Dynamic.global.Date.now().asInstanceOf[Double]

  def pause: Unit = {
    sourceNode.stop()
  }

  def play: Unit = {
    if (needRestarting) { // a SourceNode can only be started once, need to create a new one
      val oldNode = sourceNode
      oldNode.disconnect() // disconnect the old node

      sourceNode = ctx.webApi.createBufferSource()
      sourceNode.loop = oldNode.loop
      sourceNode.buffer = oldNode.buffer
      sourceNode.playbackRate.value = oldNode.playbackRate.value
      sourceNode.connect(gainNode)
    }

    sourceNode.start(0, nextStartTime)
    lastStartDate = now()
    isPlaying = true

    sourceNode.onended = () => {
      isPlaying = false
      needRestarting = true
      nextStartTime = (now() - lastStartDate) / 1000.0 // msec -> sec
    }
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

  def playing: Boolean = isPlaying
}

class JsStreamingSource private[games] (ctx: WebAudioContext, pathFuture: Future[String], outputNode: js.Dynamic) extends Source {
  private val promiseReady = Promise[Unit]

  private val audio = js.Dynamic.newInstance(js.Dynamic.global.Audio)()
  private val sourceNode = ctx.webApi.createMediaElementSource(audio)
  sourceNode.connect(outputNode)

  private var isPlaying = false

  pathFuture.onSuccess {
    case path =>
      audio.src = path
  }
  pathFuture.onFailure {
    case t =>
      promiseReady.failure(t)
  }

  audio.oncanplay = () => {
    promiseReady.success((): Unit)
  }

  audio.onerror = () => {
    val msg = "Failure of streaming"

    if (!promiseReady.isCompleted) promiseReady.failure(new RuntimeException(msg))
    else println(msg)
  }

  audio.onpause = audio.onended = () => {
    isPlaying = false
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
    isPlaying = true
  }

  def pitch: Float = audio.playbackRate.asInstanceOf[Double].toFloat

  def pitch_=(pitch: Float): Unit = {
    audio.playbackRate = pitch.toDouble
  }

  def volume: Float = audio.volume.asInstanceOf[Double].toFloat

  def volume_=(volume: Float): Unit = {
    audio.volume = volume.toDouble
  }

  def playing: Boolean = isPlaying

  private[games] val ready = promiseReady.future
}

class JsSource3D private[games] (ctx: WebAudioContext, source: AbstractSource, pannerNode: js.Dynamic) extends Source3D {
  private val positionData = new Vector3f(0, 0, 0)

  // Init
  pannerNode.setPosition(positionData.x, positionData.y, positionData.z)

  def loop: Boolean = source.loop
  def loop_=(loop: Boolean): Unit = source.loop_=(loop)
  def pause: Unit = source.pause
  def pitch: Float = source.pitch
  def pitch_=(pitch: Float): Unit = source.pitch_=(pitch)
  def play: Unit = source.play
  def volume: Float = source.volume
  def volume_=(volume: Float): Unit = source.volume_=(volume)
  def playing: Boolean = source.playing

  def position: games.math.Vector3f = positionData.copy()
  def position_=(position: games.math.Vector3f): Unit = {
    Vector3f.set(position, positionData)
    pannerNode.setPosition(positionData.x, positionData.y, positionData.z)
  }
}