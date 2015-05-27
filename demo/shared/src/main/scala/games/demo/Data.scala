package games.demo

import games.math.Vector3f

import scala.collection.immutable
import scala.collection.mutable

object Data {
  val colors: immutable.Map[Int, Vector3f] = immutable.Map(
    1 -> new Vector3f(1f, 0f, 0f), // Red for player 1
    2 -> new Vector3f(0f, 0f, 1f), // Blue for player 2
    3 -> new Vector3f(0f, 1f, 0f), // Green for player 3
    4 -> new Vector3f(1f, 1f, 0f), // Yellow for player 4
    5 -> new Vector3f(0f, 1f, 1f), // Cyan for player 5
    6 -> new Vector3f(1f, 0.5f, 0f), // Orange for player 6
    7 -> new Vector3f(0.8f, 0f, 0.8f), // Purple for player 7
    8 -> new Vector3f(1f, 0f, 0.5f) // Pink for player 8
    )
}
