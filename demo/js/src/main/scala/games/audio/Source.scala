package games.audio

import scala.scalajs.js
import org.scalajs.dom

import scala.concurrent.Future
import scala.concurrent.Promise
import scalajs.concurrent.JSExecutionContext.Implicits.queue

import games.JsUtils
import games.math.Vector3f
import games.Resource

class JsBufferedSource private[games] (ctx: WebAudioContext, buffer: js.Dynamic, outputNode: js.Dynamic) extends Source {
  // Init
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

  ctx.addSource(this)

  override def close(): Unit = {
    super.close()
    ctx.removeSource(this)
  }

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
    lastStartDate = JsUtils.now()
    isPlaying = true

    sourceNode.onended = () => {
      isPlaying = false
      needRestarting = true
      nextStartTime = (JsUtils.now() - lastStartDate) / 1000.0 // msec -> sec
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

class JsStreamingSource private[games] (ctx: WebAudioContext, res: Resource, outputNode: js.Dynamic) extends Source {
  // Init
  private val path = JsUtils.pathForResource(res)
  private val promiseReady = Promise[Unit]
  private val audio = js.Dynamic.newInstance(js.Dynamic.global.Audio)()
  audio.src = path
  private val sourceNode = ctx.webApi.createMediaElementSource(audio)
  sourceNode.connect(outputNode)

  private var isPlaying = false

  ctx.addSource(this)

  override def close(): Unit = {
    super.close()
    ctx.removeSource(this)
  }

  audio.oncanplay = () => {
    promiseReady.success((): Unit)
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