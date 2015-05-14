package games.demo

import games.math._

object Physics {
  final val playerRadius = 0.5f

  /**
   * Sets an angle in degrees in the interval ]180, 180]
   */
  def angleCentered(angle: Float): Float = {
    var ret = angle
    while (ret > 180f) ret -= 360f
    while (ret <= -180f) ret += 360f
    ret
  }

  /**
   * Sets an angle in degrees in the interval [0, 360[
   */
  def anglePositive(angle: Float): Float = {
    var ret = angle
    while (ret >= 360f) ret -= 360f
    while (ret < 0f) ret += 360f
    ret
  }

  def interpol(curIn: Float, minIn: Float, maxIn: Float, startValue: Float, endValue: Float): Float = startValue + (curIn - minIn) * (endValue - startValue) / (maxIn - minIn)

  object Wall {
    var map: Map = _

    def setup(map: Map): Unit = {
      this.map = map
    }

    def playerCollision(playerPos: Vector2f): Unit = {
      for (wall <- map.tWalls) {
        if (Math.abs(wall.y - playerPos.y) < playerRadius && Math.abs(wall.x - playerPos.x) < (playerRadius + Map.roomHalfSize)) { // AABB test
          if (Math.abs(wall.x - playerPos.x) < Map.roomHalfSize) { // front contact
            playerPos.y = wall.y + playerRadius
          } else { // contact on the corner
            // What to do? There may be another wall to continue this one
          }
        }
      }

      for (wall <- map.bWalls) {
        if (Math.abs(wall.y - playerPos.y) < playerRadius && Math.abs(wall.x - playerPos.x) < (playerRadius + Map.roomHalfSize)) { // AABB test
          if (Math.abs(wall.x - playerPos.x) < Map.roomHalfSize) { // front contact
            playerPos.y = wall.y - playerRadius
          } else { // contact on the corner
            // What to do? There may be another wall to continue this one
          }
        }
      }

      for (wall <- map.lWalls) {
        if (Math.abs(wall.x - playerPos.x) < playerRadius && Math.abs(wall.y - playerPos.y) < (playerRadius + Map.roomHalfSize)) { // AABB test
          if (Math.abs(wall.y - playerPos.y) < Map.roomHalfSize) { // front contact
            playerPos.x = wall.x + playerRadius
          } else { // contact on the corner
            // What to do? There may be another wall to continue this one
          }
        }
      }

      for (wall <- map.rWalls) {
        if (Math.abs(wall.x - playerPos.x) < playerRadius && Math.abs(wall.y - playerPos.y) < (playerRadius + Map.roomHalfSize)) { // AABB test
          if (Math.abs(wall.y - playerPos.y) < Map.roomHalfSize) { // front contact
            playerPos.x = wall.x - playerRadius
          } else { // contact on the corner
            // What to do? There may be another wall to continue this one
          }
        }
      }
    }
  }
}
