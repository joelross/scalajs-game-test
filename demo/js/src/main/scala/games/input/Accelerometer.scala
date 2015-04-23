package games.input

import scala.scalajs.js
import org.scalajs.dom

import scala.concurrent.{ Future, Promise }

import games.JsUtils
import games.math.Vector3f

object AccelerometerJS {
  private def window = js.Dynamic.global.window
  private def screen = js.Dynamic.global.screen

  private var isOrientationLocked: Boolean = false

  private lazy val usingScreenOrientationItf: Boolean = JsUtils.typeName(screen.orientation) == "ScreenOrientation"

  def lockOrientation(orientation: String): Future[Unit] = {
    val screen = this.screen

    if (usingScreenOrientationItf) {
      val promise = screen.orientation.lock(orientation)
      val ret = Promise[Unit]
      promise.`then`(() => {
        ret.success((): Unit)
        isOrientationLocked = true
      })
      promise.`catch`(() => {
        ret.failure(new RuntimeException("Orientation Lock failed"))
      })
      ret.future
    } else {
      val lockFun = JsUtils.getOptional[js.Function](screen, "lockOrientation", "mozLockOrientation", "msLockOrientation")
      val lockOrientation = lockFun.getOrElse(JsUtils.featureUnsupportedFunction("Orientation Lock"))
      screen.lockOrientation = lockOrientation

      if (screen.lockOrientation(orientation).asInstanceOf[Boolean]) {
        isOrientationLocked = true
        Future.successful((): Unit)
      } else {
        Future.failed(new RuntimeException("Orientation Lock failed"))
      }
    }
  }

  def unlockOrientation(): Unit = {
    val screen = this.screen

    if (usingScreenOrientationItf) {
      screen.orientation.unlock()
      isOrientationLocked = false
    } else {
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
  }

  def currentOrientation(): String = {
    val screen = this.screen

    if (usingScreenOrientationItf) screen.orientation.`type`.asInstanceOf[String]
    else JsUtils.getOptional[String](screen, "orientation", "mozOrientation", "msOrientation").getOrElse {
      Console.err.println(JsUtils.featureUnsupportedText("Orientation Detection"))
      "landscape-primary" // Just return standard orientation if not supported
    }
  }

  def orientationLocked: Boolean = isOrientationLocked
}

class AccelerometerJS extends Accelerometer {
  private val raw: Vector3f = new Vector3f

  private var last = System.currentTimeMillis()

  private val onDeviceMotion: js.Function = (e: js.Dynamic) => {
    val acc = e.accelerationIncludingGravity
    // Correct the data according to right-hand coordinates
    raw.x = -acc.x.asInstanceOf[Double].toFloat
    raw.y = -acc.y.asInstanceOf[Double].toFloat
    raw.z = -acc.z.asInstanceOf[Double].toFloat
  }

  private val window = js.Dynamic.global.window

  // Init
  {
    window.addEventListener("devicemotion", onDeviceMotion, true)
  }

  override def close(): Unit = {
    window.removeEventListener("devicemotion", onDeviceMotion, true)
  }

  def current(): games.math.Vector3f = {
    // Adapt the data to the current orientation of the screen
    val orientation = AccelerometerJS.currentOrientation()
    orientation match {
      case "portrait-primary"    => raw.copy()
      case "portrait-secondary"  => new Vector3f(-raw.x, -raw.y, raw.z)
      case "landscape-primary"   => new Vector3f(-raw.y, raw.x, raw.z)
      case "landscape-secondary" => new Vector3f(raw.y, -raw.x, raw.z)
      case _                     => raw.copy()
    }
  }
}