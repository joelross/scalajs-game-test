package games.demo

import games.math.{ Vector3f, Matrix3f }

object Physics {
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
}