package games.math

object Utils {
  def cotan(v: Double): Double = {
    1.0 / Math.tan(v)
  }

  /**
   * Orthogonalize an existing 3x3 matrix.
   * Can be used to make sure a matrix meant to be orthogonal stays orthogonal
   * despite floating-point rounding errors (e.g. a matrix used to accumulate
   * a lot of rotations)
   */
  def orthogonalize(mat: Matrix3f): Unit = {
    // Maybe a better way here: http://stackoverflow.com/questions/23080791/eigen-re-orthogonalization-of-rotation-matrix
    val r1 = mat.column(0)
    val r2 = mat.column(1)
    val r3 = mat.column(2)

    r1.normalise()

    val newR2 = r2 - r1 * (r1 * r2)
    newR2.normalise()

    val newR3 = r1.cross(newR2)
    newR3.normalise()

    Matrix3f.setColumn(r1, mat, 0)
    Matrix3f.setColumn(newR2, mat, 1)
    Matrix3f.setColumn(newR3, mat, 2)
  }

}
