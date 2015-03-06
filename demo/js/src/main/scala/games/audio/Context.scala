package games.audio

import scala.scalajs.js
import org.scalajs.dom
import games.Resource
import games.math.Vector3f

import js.Dynamic.{ global => g }

class JsContext extends Context {
  val audioContext: js.Dynamic = {
    val classicAudioContext: js.UndefOr[js.Dynamic] = g.AudioContext
    classicAudioContext.toOption match {
      case Some(x) => x
      case None => {
        val webKitAudioContext: js.UndefOr[js.Dynamic] = g.webkitAudioContext
        webKitAudioContext.toOption match {
          case Some(x) => x
          case None    => throw new RuntimeException("Web Audio API not supported by your browser")
        }
      }
    }
  }
  private[games] val webApi = js.Dynamic.newInstance(audioContext)()

  def createBufferedData(res: Resource): BufferedData = new JsBufferedData(this, res)
  def createStreamingData(res: Resource): StreamingData = new JsStreamingData(this, res)
  def createRawData(): RawData = ???

  def listener: Listener = new JsListener(webApi.listener)
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