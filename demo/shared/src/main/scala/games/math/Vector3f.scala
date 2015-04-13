package games.math

import java.nio.FloatBuffer

class Vector3f extends Vector {
  var x, y, z: Float = _

  def this(v1: Float, v2: Float, v3: Float) = {
    this()
    x = v1
    y = v2
    z = v3
  }

  def this(v: Vector3f) = {
    this()
    Vector3f.set(v, this)
  }

  def apply(pos: Int): Float = pos match {
    case 0 => x
    case 1 => y
    case 2 => z
    case _ => throw new IndexOutOfBoundsException
  }

  def update(pos: Int, v: Float): Unit = pos match {
    case 0 => x = v
    case 1 => y = v
    case 2 => z = v
    case _ => throw new IndexOutOfBoundsException
  }

  def load(src: FloatBuffer): Vector3f = {
    x = src.get
    y = src.get
    z = src.get
    this
  }
  def store(dst: FloatBuffer): Vector3f = {
    dst.put(x)
    dst.put(y)
    dst.put(z)
    this
  }

  def normalise(): Vector3f = {
    val l = length
    this /= l
    this
  }

  def normalizedCopy(): Vector3f = {
    val l = length
    this / l
  }

  def negate(): Vector3f = {
    Vector3f.negate(this, this)
    this
  }

  def negatedCopy(): Vector3f = {
    val ret = new Vector3f
    Vector3f.negate(this, ret)
    ret
  }

  def lengthSquared(): Float = {
    x * x + y * y + z * z
  }
  def length(): Float = {
    Math.sqrt(this.lengthSquared).toFloat
  }

  def copy(): Vector3f = {
    val ret = new Vector3f
    Vector3f.set(this, ret)
    ret
  }

  def +(v: Vector3f): Vector3f = {
    val ret = new Vector3f
    Vector3f.add(this, v, ret)
    ret
  }

  def -(v: Vector3f): Vector3f = {
    val ret = new Vector3f
    Vector3f.sub(this, v, ret)
    ret
  }

  def *(v: Vector3f): Float = {
    Vector3f.dot(this, v)
  }

  def dot(v: Vector3f): Float = {
    Vector3f.dot(this, v)
  }

  def cross(v: Vector3f): Vector3f = {
    val ret = new Vector3f
    Vector3f.cross(this, v, ret)
    ret
  }

  def x(v: Vector3f): Vector3f = {
    val ret = new Vector3f
    Vector3f.cross(this, v, ret)
    ret
  }

  def `x=`(v: Vector3f): Vector3f = {
    Vector3f.cross(this, v, this)
    this
  }

  def *(v: Float): Vector3f = {
    val ret = new Vector3f
    Vector3f.mult(this, v, ret)
    ret
  }

  def /(v: Float): Vector3f = {
    val ret = new Vector3f
    Vector3f.div(this, v, ret)
    ret
  }

  def +=(v: Vector3f): Unit = {
    Vector3f.add(this, v, this)
  }

  def -=(v: Vector3f): Unit = {
    Vector3f.sub(this, v, this)
  }

  def *=(v: Float): Unit = {
    Vector3f.mult(this, v, this)
  }

  def /=(v: Float): Unit = {
    Vector3f.div(this, v, this)
  }

  def toHomogeneous(): Vector4f = {
    val ret = new Vector4f
    Vector3f.setHomogeneous(this, ret)
    ret
  }

  override def toString = {
    "Vector3f[" + x + ", " + y + ", " + z + "]"
  }

  override def equals(obj: Any): Boolean = {
    if (obj == null) false
    if (!obj.isInstanceOf[Vector3f]) false

    val o = obj.asInstanceOf[Vector3f]

    x == o.x &&
      y == o.y &&
      z == o.z
  }

  override def hashCode(): Int = {
    x.hashCode ^
      y.hashCode ^
      z.hashCode
  }
}

object Vector3f {
  def set(src: Vector3f, dst: Vector3f): Unit = {
    dst.x = src.x
    dst.y = src.y
    dst.z = src.z
  }

  def setHomogeneous(src: Vector3f, dst: Vector4f): Unit = {
    dst.x = src.x
    dst.y = src.y
    dst.z = src.z
    dst.z = 1f
  }

  def negate(v1: Vector3f, dst: Vector3f): Unit = {
    dst.x = -v1.x
    dst.y = -v1.y
    dst.z = -v1.z
  }

  def add(v1: Vector3f, v2: Vector3f, dst: Vector3f): Unit = {
    dst.x = v1.x + v2.x
    dst.y = v1.y + v2.y
    dst.z = v1.z + v2.z
  }

  def sub(v1: Vector3f, v2: Vector3f, dst: Vector3f): Unit = {
    dst.x = v1.x - v2.x
    dst.y = v1.y - v2.y
    dst.z = v1.z - v2.z
  }

  def dot(v1: Vector3f, v2: Vector3f): Float = {
    v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
  }

  def cross(left: Vector3f, right: Vector3f, dst: Vector3f): Unit = {
    val x = left.y * right.z - left.z * right.y
    val y = right.x * left.z - right.z * left.x
    val z = left.x * right.y - left.y * right.x

    dst.x = x
    dst.y = y
    dst.z = z
  }

  def mult(v1: Vector3f, v: Float, dst: Vector3f): Unit = {
    dst.x = v1.x * v
    dst.y = v1.y * v
    dst.z = v1.z * v
  }

  def div(v1: Vector3f, v: Float, dst: Vector3f): Unit = {
    dst.x = v1.x / v
    dst.y = v1.y / v
    dst.z = v1.z / v
  }

  def Right = new Vector3f(1, 0, 0)
  def Up = new Vector3f(0, 1, 0)
  def Back = new Vector3f(0, 0, 1)
  def Left = new Vector3f(-1, 0, 0)
  def Down = new Vector3f(0, -1, 0)
  def Front = new Vector3f(0, 0, -1)
}
