package games.audio

import scala.scalajs.js
import org.scalajs.dom
import games.Resource
import games.math.Vector3f

import java.nio.ByteBuffer

import js.Dynamic.{ global => g }

class WebAudioContext extends Context {

  val classicAudioContext: js.UndefOr[js.Dynamic] = g.AudioContext
  val webKitAudioContext: js.UndefOr[js.Dynamic] = g.webkitAudioContext
  val audioContext: js.Dynamic = classicAudioContext.orElse(webKitAudioContext).getOrElse(throw new RuntimeException("Web Audio API not supported by your browser"))
  private[games] val webApi = js.Dynamic.newInstance(audioContext)()

  private[games] val mainOutput = {
    val node = webApi.createGain()
    node.connect(webApi.destination)
    node.gain.value = 1.0
    node
  }

  def createBufferedData(res: Resource): BufferedData = new JsBufferedData(this, res)
  def createStreamingData(res: Resource): StreamingData = new JsStreamingData(this, res)
  def createRawData(data: ByteBuffer, format: Format, channels: Int, freq: Int): RawData = new JsRawData(this, data, format, channels, freq)

  def listener: Listener = new JsListener(webApi.listener)

  def volume: Float = mainOutput.gain.value.asInstanceOf[Double].toFloat
  def volume_=(volume: Float) = {
    mainOutput.gain.value = volume.toDouble
  }
}

class JsListener private[games] (webListener: js.Dynamic) extends Listener {
  private val orientationData = new Vector3f(0, 0, -1)
  private val upData = new Vector3f(0, 1, 0)
  private val positionData = new Vector3f(0, 0, 0)

  // Init
  webListener.setPosition(positionData.x, positionData.y, positionData.z)
  webListener.setOrientation(orientationData.x, orientationData.y, orientationData.z, upData.x, upData.y, upData.z)

  def orientation: Vector3f = orientationData.copy()
  def orientation_=(orientation: Vector3f): Unit = {
    Vector3f.set(orientation, orientationData)
    webListener.setOrientation(orientationData.x, orientationData.y, orientationData.z, upData.x, upData.y, upData.z)
  }
  def position: Vector3f = positionData.copy()
  def position_=(position: Vector3f): Unit = {
    Vector3f.set(position, positionData)
    webListener.setPosition(positionData.x, positionData.y, positionData.z)
  }
  def up: Vector3f = upData.copy()
  def up_=(up: Vector3f): Unit = {
    Vector3f.set(up, upData)
    webListener.setOrientation(orientationData.x, orientationData.y, orientationData.z, upData.x, upData.y, upData.z)
  }
}