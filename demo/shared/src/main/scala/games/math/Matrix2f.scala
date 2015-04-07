package games.math

import java.nio.FloatBuffer

/**
 * Ported from LWJGL source code
 */
class Matrix2f extends Matrix {
  private[math] var m00, m11: Float = 1
  private[math] var m01, m10: Float = 0

  def this(a00: Float, a01: Float, a10: Float, a11: Float) = {
    this()
    // Internally stored as Column-major
    m00 = a00
    m01 = a10
    m10 = a01
    m11 = a11
  }

  def this(m: Matrix2f) = {
    this()
    Matrix2f.set(m, this)
  }

  def apply(row: Int, col: Int): Float = (row, col) match {
    case (0, 0) => m00
    case (0, 1) => m10
    case (1, 0) => m01
    case (1, 1) => m11
    case _      => throw new IndexOutOfBoundsException
  }

  def update(row: Int, col: Int, v: Float): Unit = (row, col) match {
    case (0, 0) => m00 = v
    case (0, 1) => m10 = v
    case (1, 0) => m01 = v
    case (1, 1) => m11 = v
    case _      => throw new IndexOutOfBoundsException
  }

  def load(src: FloatBuffer, order: MajorOrder): Matrix2f = order match {
    case RowMajor =>
      m00 = src.get()
      m10 = src.get()
      m01 = src.get()
      m11 = src.get()
      this
    case ColumnMajor =>
      m00 = src.get()
      m01 = src.get()
      m10 = src.get()
      m11 = src.get()
      this
  }
  def store(dst: FloatBuffer, order: MajorOrder): Matrix2f = order match {
    case RowMajor =>
      dst.put(m00)
      dst.put(m10)
      dst.put(m01)
      dst.put(m11)
      this
    case ColumnMajor =>
      dst.put(m00)
      dst.put(m01)
      dst.put(m10)
      dst.put(m11)
      this
  }

  def setIdentity(): Matrix2f = {
    m00 = 1
    m01 = 0
    m10 = 0
    m11 = 1
    this
  }
  def setZero(): Matrix2f = {
    m00 = 0
    m01 = 0
    m10 = 0
    m11 = 0
    this
  }

  def invert(): Matrix2f = {
    Matrix2f.invert(this, this)
    this
  }
  def invertedCopy(): Matrix2f = {
    val ret = new Matrix2f
    Matrix2f.invert(this, ret)
    ret
  }

  def negate(): Matrix2f = {
    Matrix2f.negate(this, this)
    this
  }
  def negatedCopy(): Matrix2f = {
    val ret = new Matrix2f
    Matrix2f.negate(this, ret)
    ret
  }

  def transpose(): Matrix2f = {
    Matrix2f.transpose(this, this)
    this
  }
  def transposedCopy(): Matrix2f = {
    val ret = new Matrix2f
    Matrix2f.transpose(this, ret)
    ret
  }

  def determinant(): Float = {
    m00 * m11 - m01 * m10
  }

  def copy(): Matrix2f = {
    val ret = new Matrix2f
    Matrix2f.set(this, ret)
    ret
  }

  def +(m: Matrix2f): Matrix2f = {
    val ret = new Matrix2f
    Matrix2f.add(this, m, ret)
    ret
  }

  def +=(m: Matrix2f): Matrix2f = {
    Matrix2f.add(this, m, this)
    this
  }

  def -(m: Matrix2f): Matrix2f = {
    val ret = new Matrix2f
    Matrix2f.sub(this, m, ret)
    ret
  }

  def -=(m: Matrix2f): Matrix2f = {
    Matrix2f.sub(this, m, this)
    this
  }

  def *(m: Matrix2f): Matrix2f = {
    val ret = new Matrix2f
    Matrix2f.mult(this, m, ret)
    ret
  }

  def *=(m: Matrix2f): Matrix2f = {
    Matrix2f.mult(this, m, this)
    this
  }

  def *(v: Float): Matrix2f = {
    val ret = new Matrix2f
    Matrix2f.mult(this, v, ret)
    ret
  }

  def *=(v: Float): Matrix2f = {
    Matrix2f.mult(this, v, this)
    this
  }

  def /(v: Float): Matrix2f = {
    val ret = new Matrix2f
    Matrix2f.div(this, v, ret)
    ret
  }

  def /=(v: Float): Matrix2f = {
    Matrix2f.div(this, v, this)
    this
  }

  def *(v: Vector2f): Vector2f = {
    val ret = new Vector2f
    Matrix2f.mult(this, v, ret)
    ret
  }

  def transform(v: Vector2f): Vector2f = {
    val ret = new Vector2f
    Matrix2f.mult(this, v, ret)
    ret
  }

  def toHomogeneous(): Matrix3f = {
    val ret = new Matrix3f
    Matrix2f.setHomogeneous(this, ret)
    ret
  }

  override def toString: String = {
    var sb = ""
    sb += m00 + " " + m10 + "\n"
    sb += m01 + " " + m11 + "\n"
    sb
  }

  override def equals(obj: Any): Boolean = {
    if (obj == null) false
    if (!obj.isInstanceOf[Matrix2f]) false

    val o = obj.asInstanceOf[Matrix2f]

    m00 == o.m00 &&
      m01 == o.m01 &&
      m10 == o.m10 &&
      m11 == o.m11
  }

  override def hashCode(): Int = {
    m00.toInt ^ m01.toInt ^ m10.toInt ^ m11.toInt
  }
}

object Matrix2f {
  def set(src: Matrix2f, dst: Matrix2f): Unit = {
    dst.m00 = src.m00
    dst.m01 = src.m01
    dst.m10 = src.m10
    dst.m11 = src.m11
  }

  def setColumn(src: Matrix2f, colIdx: Int, dst: Vector2f): Unit = colIdx match {
    case 0 =>
      dst.x = src.m00
      dst.y = src.m01

    case 1 =>
      dst.x = src.m10
      dst.y = src.m11

    case _ => throw new IndexOutOfBoundsException
  }

  def setRow(src: Matrix2f, rowIdx: Int, dst: Vector2f): Unit = rowIdx match {
    case 0 =>
      dst.x = src.m00
      dst.y = src.m10

    case 1 =>
      dst.x = src.m01
      dst.y = src.m11

    case _ => throw new IndexOutOfBoundsException
  }

  def setHomogeneous(src: Matrix2f, dst: Matrix3f): Unit = {
    dst.m00 = src.m00
    dst.m01 = src.m01
    dst.m02 = 0f

    dst.m10 = src.m10
    dst.m11 = src.m11
    dst.m12 = 0f

    dst.m20 = 0f
    dst.m21 = 0f
    dst.m22 = 1f
  }

  def negate(src: Matrix2f, dst: Matrix2f): Unit = {
    dst.m00 = -src.m00
    dst.m01 = -src.m01
    dst.m10 = -src.m10
    dst.m11 = -src.m11
  }

  def invert(src: Matrix2f, dst: Matrix2f): Unit = {
    val det = src.determinant

    if (det != 0) {
      val det_inv = 1f / det

      val t00 = src.m11 * det_inv
      val t01 = -src.m01 * det_inv
      val t11 = src.m00 * det_inv
      val t10 = -src.m10 * det_inv

      dst.m00 = t00
      dst.m01 = t01
      dst.m10 = t10
      dst.m11 = t11
    }
  }

  def transpose(src: Matrix2f, dst: Matrix2f): Unit = {
    val t10 = src.m10
    val t01 = src.m01

    dst.m00 = src.m00
    dst.m01 = t10
    dst.m10 = t01
    dst.m11 = src.m11
  }

  def add(m1: Matrix2f, m2: Matrix2f, dst: Matrix2f): Unit = {
    dst.m00 = m1.m00 + m2.m00
    dst.m01 = m1.m10 + m2.m10
    dst.m10 = m1.m01 + m2.m01
    dst.m11 = m1.m11 + m2.m11
  }

  def sub(m1: Matrix2f, m2: Matrix2f, dst: Matrix2f): Unit = {
    dst.m00 = m1.m00 - m2.m00
    dst.m01 = m1.m10 - m2.m10
    dst.m10 = m1.m01 - m2.m01
    dst.m11 = m1.m11 - m2.m11
  }

  def mult(left: Matrix2f, right: Matrix2f, dst: Matrix2f): Unit = {
    val m00 = left.m00 * right.m00 + left.m10 * right.m01
    val m01 = left.m01 * right.m00 + left.m11 * right.m01
    val m10 = left.m00 * right.m10 + left.m10 * right.m11
    val m11 = left.m01 * right.m10 + left.m11 * right.m11

    dst.m00 = m00
    dst.m01 = m01
    dst.m10 = m10
    dst.m11 = m11
  }

  def mult(left: Matrix2f, right: Vector2f, dst: Vector2f): Unit = {
    val x = left.m00 * right.x + left.m10 * right.y
    val y = left.m01 * right.x + left.m11 * right.y

    dst.x = x
    dst.y = y
  }

  def mult(left: Matrix2f, right: Float, dst: Matrix2f): Unit = {
    dst.m00 = left.m00 * right
    dst.m01 = left.m01 * right
    dst.m10 = left.m10 * right
    dst.m11 = left.m11 * right
  }

  def div(left: Matrix2f, right: Float, dst: Matrix2f): Unit = {
    dst.m00 = left.m00 / right
    dst.m01 = left.m01 / right
    dst.m10 = left.m10 / right
    dst.m11 = left.m11 / right
  }

  /**
   * Generates the non-homogeneous rotation matrix for a given angle (in degrees) around the origin
   */
  def rotation2D(angle: Float): Matrix2f = {
    val ret = new Matrix2f
    setRotation2D(angle, ret)
    ret
  }

  def setRotation2D(angle: Float, dst: Matrix2f): Unit = {
    val radAngle = Utils.degToRad(angle)

    val c = Math.cos(radAngle).toFloat
    val s = Math.sin(radAngle).toFloat

    dst.m00 = c
    dst.m10 = -s

    dst.m01 = s
    dst.m11 = c
  }

  /**
   * Generates the non-homogeneous scaling matrix for a given scale vector around the origin
   */
  def scale2D(scale: Vector2f): Matrix2f = {
    val ret = new Matrix2f
    setScale2D(scale, ret)
    ret
  }

  def setScale2D(scale: Vector2f, dst: Matrix2f): Unit = {
    dst.m00 = scale.x
    dst.m10 = 0f

    dst.m01 = 0f
    dst.m11 = scale.y
  }
}
