package games.input

import scala.scalajs.js
import org.scalajs.dom

import games.JsUtils

object AccelerometerJS {
  private def window = js.Dynamic.global.window
  private def screen = js.Dynamic.global.screen

  def lockOrientation(orientation: String): Unit = {
    val lockFun = JsUtils.getOptional[js.Function1[Unit, String]](screen, "lockOrientation", "mozLockOrientation", "msLockOrientation")
    lockFun match {
      case Some(fun) => fun(orientation)
      case None      => JsUtils.throwFeatureUnsupported("Orientation Lock (Request)")
    }
  }

  def unlockOrientation(): Unit = {
    val unlockFun = JsUtils.getOptional[js.Function0[Unit]](screen, "lockOrientation", "mozLockOrientation", "msLockOrientation")
    unlockFun match {
      case Some(fun) => fun()
      case None      => JsUtils.throwFeatureUnsupported("Orientation Lock (Exit)")
    }
  }

  def currentOrientation(): String = {
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
    orientation.getOrElse(JsUtils.throwFeatureUnsupported("Orientation Detection"))
  }
}

class AccelerometerJS extends Accelerometer {
  private val onDeviceMotion: js.Function = (e: js.Dynamic) => {}

  override def close(): Unit = {

  }

  def current(): games.input.Acceleration = ???
}