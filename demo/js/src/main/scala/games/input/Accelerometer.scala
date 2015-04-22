package games.input

import scala.scalajs.js
import org.scalajs.dom

import games.JsUtils

class AccelerometerJS extends Accelerometer {
  private val onDeviceMotion: js.Function = (e: js.Dynamic) => {}

  override def close(): Unit = {

  }

  def current(): games.input.Acceleration = ???
}