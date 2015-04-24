package games.math

object Utils {
  def cotan(v: Double): Double = {
    1.0 / Math.tan(v)
  }

  def orthogonalize(mat: Matrix3f): Unit = {
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
