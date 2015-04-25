package games.audio

import scala.scalajs.js
import org.scalajs.dom
import games.Resource
import games.math.Vector3f
import games.JsUtils

import java.nio.ByteBuffer

import scala.collection.mutable.Set

import js.Dynamic.{ global => g }

object WebAudioContext {
  lazy val canPlayOgg: Boolean = {
    // From http://diveintohtml5.info/everything.html
    val audio = dom.document.createElement("audio").asInstanceOf[js.Dynamic]
    val typeRegex = js.Dynamic.newInstance(g.RegExp)("/ably/")
    JsUtils.getOptional[js.Function](audio, "canPlayType").map { _ =>
      val canPlay = audio.canPlayType("audio/ogg; codecs=\"vorbis\"").asInstanceOf[String].toLowerCase()
      !(canPlay.isEmpty() || canPlay == "no")
    }.getOrElse(false)
  }

  lazy val canUseAurora: Boolean = {
    JsUtils.getOptional(js.Dynamic.global, "AV").isDefined
  }
}

class WebAudioContext extends Context {
  val audioContext: js.Dynamic = JsUtils.getOptional[js.Dynamic](g, "AudioContext", "webkitAudioContext").getOrElse(throw new RuntimeException("Web Audio API not supported by your browser"))
  private[games] val webApi = js.Dynamic.newInstance(audioContext)()

  private[games] val mainOutput = {
    val node = webApi.createGain()
    node.connect(webApi.destination)
    node.gain.value = 1.0
    node
  }

  def createBufferedData(res: Resource): games.audio.Data = new JsBufferedData(this, res)
  def createStreamingData(res: Resource): games.audio.Data = {
    // Streaming data is not a good idea on Android Chrome: https://code.google.com/p/chromium/issues/detail?id=138132#c6
    if (JsUtils.Browser.chrome && JsUtils.Browser.android) {
      Console.err.println("Warning: Android Chrome does not support streaming data, switching to buffered data")
      this.createBufferedData(res)
    } else new JsStreamingData(this, res)
  }
  def createRawData(data: ByteBuffer, format: Format, channels: Int, freq: Int): games.audio.Data = new JsRawData(this, data, format, channels, freq)

  override def close(): Unit = {
    super.close()
    sources.foreach { source => source.close() }
    sources.clear()
  }

  val listener: Listener = new JsListener(this)

  def volume: Float = mainOutput.gain.value.asInstanceOf[Double].toFloat
  def volume_=(volume: Float) = {
    mainOutput.gain.value = volume.toDouble
  }

  private val sources: Set[AbstractSource] = Set()
  private[games] def addSource(source: AbstractSource): Unit = {
    sources += source
  }
  private[games] def removeSource(source: AbstractSource): Unit = {
    sources -= source
  }
}

class JsListener private[games] (ctx: WebAudioContext) extends Listener {
  private val orientationData = new Vector3f(0, 0, -1)
  private val upData = new Vector3f(0, 1, 0)
  private val positionData = new Vector3f(0, 0, 0)

  // Init
  ctx.webApi.listener.setPosition(positionData.x.toDouble, positionData.y.toDouble, positionData.z.toDouble)
  ctx.webApi.listener.setOrientation(orientationData.x.toDouble, orientationData.y.toDouble, orientationData.z.toDouble, upData.x.toDouble, upData.y.toDouble, upData.z.toDouble)

  def orientation: Vector3f = orientationData.copy()
  def position: Vector3f = positionData.copy()
  def position_=(position: Vector3f): Unit = {
    Vector3f.set(position, positionData)
    ctx.webApi.listener.setPosition(positionData.x.toDouble, positionData.y.toDouble, positionData.z.toDouble)
  }
  def up: Vector3f = upData.copy()
  def setOrientation(orientation: Vector3f, up: Vector3f): Unit = {
    Vector3f.set(orientation, orientationData)
    Vector3f.set(up, upData)
    ctx.webApi.listener.setOrientation(orientationData.x.toDouble, orientationData.y.toDouble, orientationData.z.toDouble, upData.x.toDouble, upData.y.toDouble, upData.z.toDouble)
  }
}