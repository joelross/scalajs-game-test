package games.demo

import games.math.{ Vector3f, Matrix3f }

object Physics {
  val maxAngleY: Float = 30f
  val maxRotationXSpeed: Float = 100f
  val maxRotationYSpeed: Float = 100f
  val shipAngleAtMaxRotationXSpeed: Float = 45f

  val bulletVelocity: Float = 10f
  val bulletRotation: Float = 360f

  /**
   * Sets an angle in degrees in the interval ]180, 180]
   */
  def angleCentered(angle: Float): Float = {
    var ret = angle
    while (ret > 180f) ret -= 360f
    while (ret <= -180f) ret += 360f
    ret
  }

  /**
   * Sets an angle in degrees in the interval [0, 360[
   */
  def anglePositive(angle: Float): Float = {
    var ret = angle
    while (ret >= 360f) ret -= 360f
    while (ret < 0f) ret += 360f
    ret
  }

  def interpol(curIn: Float, minIn: Float, maxIn: Float, startValue: Float, endValue: Float): Float = startValue + (curIn - minIn) * (endValue - startValue) / (maxIn - minIn)
}
