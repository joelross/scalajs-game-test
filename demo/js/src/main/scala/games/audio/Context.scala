package games.audio

import scala.scalajs.js
import org.scalajs.dom
import games.Resource
import games.math.Vector3f
import games.JsUtils

import java.nio.ByteBuffer

import scala.collection.mutable.Set
import scala.concurrent.Future

import js.Dynamic.{ global => g }

object WebAudioContext {
  lazy val auroraPresent: Boolean = {
    JsUtils.getOptional[js.Dynamic](js.Dynamic.global, "AV").flatMap { av => JsUtils.getOptional[js.Dynamic](av, "Asset") }.isDefined
  }

  def canUseAurora: Boolean = JsUtils.useAuroraJs && auroraPresent
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

  def prepareStreamingData(res: Resource): Future[games.audio.Data] = {
    // Streaming data is not a good idea on Android Chrome: https://code.google.com/p/chromium/issues/detail?id=138132#c6
    if (JsUtils.Browser.chrome && JsUtils.Browser.android) {
      Console.err.println("Warning: Android Chrome does not support streaming data (resource " + res + "), switching to buffered data")
      this.prepareBufferedData(res)
    } else Future.successful(new JsStreamingData(this, res))
  }
  def prepareBufferedData(res: Resource): Future[games.audio.BufferedData] = ???
  def prepareRawData(data: ByteBuffer, format: Format, channels: Int, freq: Int): Future[games.audio.BufferedData] = ???

  def createSource(): Source = new JsSource(this, mainOutput)
  def createSource3D(): Source3D = new JsSource3D(this, mainOutput)

  val listener: Listener = new JsListener(this)

  def volume: Float = mainOutput.gain.value.asInstanceOf[Double].toFloat
  def volume_=(volume: Float) = mainOutput.gain.value = volume.toDouble

  override def close(): Unit = {
    super.close()
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