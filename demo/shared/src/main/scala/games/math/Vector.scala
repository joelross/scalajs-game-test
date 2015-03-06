package games.math

import java.nio.FloatBuffer

abstract class Vector {
  def apply(pos: Int): Float
  def update(pos: Int, v: Float): Unit

  def load(src: FloatBuffer): Vector
  def store(dst: FloatBuffer): Vector

  def normalise(): Vector
  def normalizedCopy(): Vector

  def negate(): Vector
  def negatedCopy(): Vector

  def lengthSquared(): Float
  def length(): Float

  def copy(): Vector
}
