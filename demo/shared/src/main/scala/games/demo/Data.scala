package games.demo

import games.math.Vector3f

import scala.collection.immutable
import scala.collection.mutable

object Data {
  val colors: immutable.Map[Int, Vector3f] = immutable.Map(1 -> new Vector3f(1, 0, 0), // Red for player 1
    2 -> new Vector3f(0, 0, 1), // Blue for player 2
    3 -> new Vector3f(0, 1, 0), // Green for player 3
    4 -> new Vector3f(1, 1, 0)) // Yellow for player 4

  val initOrientation: immutable.Map[Int, Float] = immutable.Map(1 -> 180f, 2 -> 0f, 3 -> 270f, 4 -> 90f)
}
