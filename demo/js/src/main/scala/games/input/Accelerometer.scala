package games.input

import scala.scalajs.js
import org.scalajs.dom

import games.JsUtils

object AccelerometerJS {
  private def window = js.Dynamic.global.window
  private def screen = js.Dynamic.global.screen

  private var isOrientationLocked: Boolean = false

  def orientationLocked: Boolean = isOrientationLocked

  def lockOrientation(orientation: String): Unit = {
    val screen = this.screen
    val lockFun = JsUtils.getOptional[js.Function](screen, "lockOrientation", "mozLockOrientation", "msLockOrientation")
    val lockOrientation = lockFun.getOrElse(JsUtils.featureUnsupportedFunction("Orientation Lock"))
    screen.lockOrientation = lockOrientation

    if (screen.lockOrientation(orientation).asInstanceOf[Boolean]) {
      isOrientationLocked = true
    } else {
      Console.err.println("Orientation Lock failed")
    }
  }

  def unlockOrientation(): Unit = {
    val screen = this.screen
    val unlockFun = JsUtils.getOptional[js.Function](screen, "unlockOrientation", "mozUnlockOrientation", "msUnlockOrientation")
    val unlockOrientation = unlockFun.getOrElse(JsUtils.featureUnsupportedFunction("Orientation Unlock"))
    screen.unlockOrientation = unlockOrientation

    val retVal = screen.unlockOrientation()
    JsUtils.typeName(retVal) match {
      case "Boolean" =>
        val boolRetVal = retVal.asInstanceOf[Boolean]
        if (boolRetVal) {
          isOrientationLocked = false
        } else {
          Console.err.println("Orientation Unlock failed")
        }
      case x =>
        isOrientationLocked = false // Just assume it went fine...
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
    orientation.getOrElse {
      Console.err.println(JsUtils.featureUnsupportedText("Orientation Detection"))
      "landscape-primary" // Just return standard orientation if not supported
    }
  }
}

class AccelerometerJS extends Accelerometer {
  private val onDeviceMotion: js.Function = (e: js.Dynamic) => {}

  override def close(): Unit = {

  }

  def current(): games.input.Acceleration = ???
}