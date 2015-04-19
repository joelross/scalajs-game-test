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
   * Orientation:
   * x is the horizontal direction the object is looking at
   * y is the vertical direction the object is looking at
   * z is the angle the object is rotated on itself
   * (Yeah, it's basically Euler angles)
   * Could be optimized: http://en.wikipedia.org/wiki/Euler_angles#Rotation_matrix (see Y1X2Z3 with inverted Z)
   */
  def matrixForOrientation(orientation: Vector3f): Matrix3f = Matrix3f.rotation3D(orientation.x, Vector3f.Up) *
    Matrix3f.rotation3D(orientation.y, Vector3f.Right) * Matrix3f.rotation3D(orientation.z, Vector3f.Front)

  def interpol(curIn: Float, minIn: Float, maxIn: Float, startValue: Float, endValue: Float): Float = startValue + (curIn - minIn) * (endValue - startValue) / (maxIn - minIn)

  def stepShip(elapsedSinceLastFrame: Float, data: ShipData): Unit = {
    data.orientation.x += data.rotation.x * elapsedSinceLastFrame
    data.orientation.y += data.rotation.y * elapsedSinceLastFrame
    if (Math.abs(data.orientation.y) > maxAngleY) {
      data.orientation.y = Math.signum(data.orientation.y) * maxAngleY
      data.rotation.y = 0
    }

    val localOrientationMatrix = Physics.matrixForOrientation(data.orientation)
    data.position += localOrientationMatrix * (Vector3f.Front * (data.velocity * elapsedSinceLastFrame))

    data.orientation.z = 0.9f * data.orientation.z + 0.1f * interpol(data.rotation.x, -maxRotationXSpeed, +maxRotationXSpeed, +shipAngleAtMaxRotationXSpeed, -shipAngleAtMaxRotationXSpeed)
  }

  def stepBullet(elapsedSinceLastFrame: Float, data: BulletData): Unit = {
    data.orientation.z += bulletRotation * elapsedSinceLastFrame
    val localOrientationMatrix = Physics.matrixForOrientation(data.orientation)
    data.position += localOrientationMatrix * (Vector3f.Front * (bulletVelocity * elapsedSinceLastFrame))
  }
}
