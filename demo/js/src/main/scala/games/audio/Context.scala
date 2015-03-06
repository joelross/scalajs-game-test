package games.audio

import scala.scalajs.js
import org.scalajs.dom
import games.Resource

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

  def createBufferedData(res: Resource) = new JsBufferedData(this, res)
  def createStreamingData(res: Resource) = new JsStreamingData(this, res)
  def createRawData() = new JsRawData(this)
  
  def listener: Listener = ???
}