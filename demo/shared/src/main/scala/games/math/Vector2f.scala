package games.math

import java.nio.FloatBuffer

class Vector2f extends Vector {
  var x, y: Float = _

  def this(v1: Float, v2: Float) = {
    this()
    x = v1
    y = v2
  }

  def this(v: Vector2f) = {
    this()
    Vector2f.set(v, this)
  }

  def apply(pos: Int): Float = pos match {
    case 0 => x
    case 1 => y
    case _ => throw new IndexOutOfBoundsException
  }

  def update(pos: Int, v: Float): Unit = pos match {
    case 0 => x = v
    case 1 => y = v
    case _ => throw new IndexOutOfBoundsException
  }

  def load(src: FloatBuffer): Vector2f = {
    x = src.get
    y = src.get
    this
  }
  def store(dst: FloatBuffer): Vector2f = {
    dst.put(x)
    dst.put(y)
    this
  }

  def normalise(): Vector2f = {
    val l = length
    this /= l
    this
  }

  def normalizedCopy(): Vector2f = {
    val l = length
    this / l
  }

  def negate(): Vector2f = {
    Vector2f.negate(this, this)
    this
  }

  def negatedCopy(): Vector2f = {
    val ret = new Vector2f
    Vector2f.negate(this, ret)
    ret
  }

  def lengthSquared(): Float = {
    x * x + y * y
  }
  def length(): Float = {
    Math.sqrt(this.lengthSquared).toFloat
  }

  def copy(): Vector2f = {
    val ret = new Vector2f
    Vector2f.set(this, ret)
    ret
  }

  def +(v: Vector2f): Vector2f = {
    val ret = new Vector2f
    Vector2f.add(this, v, ret)
    ret
  }

  def -(v: Vector2f): Vector2f = {
    val ret = new Vector2f
    Vector2f.sub(this, v, ret)
    ret
  }

  def *(v: Vector2f): Float = {
    Vector2f.dot(this, v)
  }

  def dot(v: Vector2f): Float = {
    Vector2f.dot(this, v)
  }

  def *(v: Float): Vector2f = {
    val ret = new Vector2f
    Vector2f.mult(this, v, ret)
    ret
  }

  def /(v: Float): Vector2f = {
    val ret = new Vector2f
    Vector2f.div(this, v, ret)
    ret
  }

  def +=(v: Vector2f): Unit = {
    Vector2f.add(this, v, this)
  }

  def -=(v: Vector2f): Unit = {
    Vector2f.sub(this, v, this)
  }

  def *=(v: Float): Unit = {
    Vector2f.mult(this, v, this)
  }

  def /=(v: Float): Unit = {
    Vector2f.div(this, v, this)
  }

  def toHomogeneous(): Vector3f = {
    val ret = new Vector3f
    Vector2f.setHomogeneous(this, ret)
    ret
  }

  override def toString = {
    "Vector2f[" + x + ", " + y + "]"
  }

  override def equals(obj: Any): Boolean = {
    if (obj == null) false
    if (!obj.isInstanceOf[Vector2f]) false

    val o = obj.asInstanceOf[Vector2f]

    x == o.x &&
      y == o.y
  }

  override def hashCode(): Int = {
    x.hashCode ^
      y.hashCode
  }
}

object Vector2f {
  def set(src: Vector2f, dst: Vector2f): Unit = {
    dst.x = src.x
    dst.y = src.y
  }

  def setHomogeneous(src: Vector2f, dst: Vector3f): Unit = {
    dst.x = src.x
    dst.y = src.y
    dst.z = 1f
  }

  def negate(v1: Vector2f, dst: Vector2f): Unit = {
    dst.x = -v1.x
    dst.y = -v1.y
  }

  def add(v1: Vector2f, v2: Vector2f, dst: Vector2f): Unit = {
    dst.x = v1.x + v2.x
    dst.y = v1.y + v2.y
  }

  def sub(v1: Vector2f, v2: Vector2f, dst: Vector2f): Unit = {
    dst.x = v1.x - v2.x
    dst.y = v1.y - v2.y
  }

  def dot(v1: Vector2f, v2: Vector2f): Float = {
    v1.x * v2.x + v1.y * v2.y
  }

  def mult(v1: Vector2f, v: Float, dst: Vector2f): Unit = {
    dst.x = v1.x * v
    dst.y = v1.y * v
  }

  def div(v1: Vector2f, v: Float, dst: Vector2f): Unit = {
    dst.x = v1.x / v
    dst.y = v1.y / v
  }
}
