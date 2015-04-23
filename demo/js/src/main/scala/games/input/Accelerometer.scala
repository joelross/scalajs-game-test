package games.input

import scala.scalajs.js
import org.scalajs.dom

import games.JsUtils

object AccelerometerJS {
  def currentOrientation(): Option[String] = {
    val screen = js.Dynamic.global.screen
    val orientation = JsUtils.getOptional[js.Any](screen, "orientation", "mozOrientation", "msOrientation").flatMap { data =>
      JsUtils.typeName(data) match {
        case "String"            => Some(data.asInstanceOf[String]) // Firefox
        case "ScreenOrientation" => Some(data.asInstanceOf[js.Dynamic].`type`.asInstanceOf[String]) // Chrome
        case tpe =>
          Console.err.println("Unknown orientation type: " + tpe)
          None
      }
    }
    orientation
  }
}

class AccelerometerJS extends Accelerometer {
  private val onDeviceMotion: js.Function = (e: js.Dynamic) => {}

  override def close(): Unit = {

  }

  def current(): games.input.Acceleration = ???
}