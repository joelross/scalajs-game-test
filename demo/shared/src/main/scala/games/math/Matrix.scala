package games.math

import java.nio.FloatBuffer

abstract class Matrix {
  def apply(row: Int, col: Int): Float
  def update(row: Int, col: Int, v: Float): Unit

  def load(src: FloatBuffer, order: MajorOrder): Matrix
  def store(dst: FloatBuffer, order: MajorOrder): Matrix

  def setIdentity(): Matrix
  def setZero(): Matrix

  def invert(): Matrix
  def invertedCopy(): Matrix

  def negate(): Matrix
  def negatedCopy(): Matrix

  def transpose(): Matrix
  def transposedCopy(): Matrix

  def determinant(): Float

  def copy(): Matrix
}
