package games.demo

import games.math.Vector3f

object Data {
  val colors = Map(1 -> new Vector3f(1, 0, 0), // Red for player 1
    2 -> new Vector3f(0, 0, 1), // Blue for player 2
    3 -> new Vector3f(0, 1, 0), // Green for player 3
    4 -> new Vector3f(1, 1, 0)) // Yellow for player 4
}
