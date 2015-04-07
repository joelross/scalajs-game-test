package games.math

object Utils {
  val degToRadFactor: Float = (Math.PI / 180.0).toFloat
  val radToDegFactor: Float = (180.0 / Math.PI).toFloat

  def degToRad(deg: Float): Float = {
    deg * degToRadFactor
  }

  def radToDeg(rad: Float): Float = {
    rad * radToDegFactor
  }

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

    // TODO
  }

}
