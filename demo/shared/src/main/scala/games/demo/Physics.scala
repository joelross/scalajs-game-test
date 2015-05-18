package games.demo

import games.math._

import scala.collection.{ immutable, mutable }

object Physics {
  final val playerRadius = 0.5f
  final val projectileVelocity = 10f

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

  var map: Map = _

  def setupMap(map: Map): Unit = {
    this.map = map
  }

  /*
   * -1 = no collision, 0 = wall, > 0 player id hit
   */
  def projectileStep(proj: (Int, Projectile), players: immutable.Map[Int, Playing], elapsedSinceLastFrame: Float): Int = {
    val (shooterId, projectile) = proj

    // Move the projectile
    val direction = Matrix2f.rotate2D(-projectile.orientation) * new Vector2f(0, -1)
    val distance = projectileVelocity * elapsedSinceLastFrame

    val startPoint = projectile.position
    projectile.position = projectile.position + direction * distance // new position in case of no collision

    // Collision detection
    val res = players.flatMap {
      case (playerId, player) =>
        if (shooterId != playerId) {
          // From http://mathworld.wolfram.com/Circle-LineIntersection.html

          val x1 = startPoint.x - player.position.x
          val y1 = startPoint.y - player.position.y

          val dx = direction.x
          val dy = direction.y

          val x2 = x1 + dx
          val y2 = y1 + dy

          val r = playerRadius
          // dr is always 1 (dx and dy are part of a unit vector)
          val d = x1 * y2 - x2 * y1

          val disc = r * r - d * d

          if (disc >= 0f) {
            val disc_sqrt = Math.sqrt(disc).toFloat

            val partx = Math.signum(dy) * dx * disc_sqrt
            val party = Math.abs(dy) * disc_sqrt

            // First contact point
            val cx1 = (d * dy + partx)
            val cy1 = (-d * dx + party)

            // Second contact point
            val cx2 = (d * dy - partx)
            val cy2 = (-d * dx - party)

            // Use dot product to compute distance from initial point
            val l1 = (cx1 - x1) * dx + (cy1 - y1) * dy
            val l2 = (cx2 - x1) * dx + (cy2 - y1) * dy

            // Check which one(s) is(are) really reached during this step
            val l1_valid = (l1 >= 0f && l1 <= distance)
            val l2_valid = (l2 >= 0f && l2 <= distance)

            if (l1_valid || l2_valid) {
              val collision_distance = if (l1_valid && l2_valid) Math.min(l1, l2) else if (l1_valid) l1 else l2
              projectile.position = startPoint + direction * collision_distance
              Some(playerId)
            } else None
          } else None
        } else None
    }

    if (!res.isEmpty) res.head else -1
  }

  def playerStep(player: Playing, elapsedSinceLastFrame: Float): Unit = {
    // Move the player
    player.position += (Matrix2f.rotate2D(-player.orientation) * player.velocity) * elapsedSinceLastFrame

    // Collision with the map
    val playerPos = player.position

    for (wall <- map.ctWalls) {
      val pos = wall.position
      val length = wall.length
      val halfLength = wall.halfLength
      if (Math.abs(pos.y - playerPos.y) < playerRadius && Math.abs(pos.x - playerPos.x) < (playerRadius + halfLength)) { // AABB test
        if (Math.abs(pos.x - playerPos.x) < halfLength) { // front contact
          playerPos.y = pos.y + playerRadius
        } else { // contact on the corner
          val cornerPos = if (playerPos.x > pos.x) { // Right corner
            pos + new Vector2f(halfLength, 0)
          } else { // Left corner
            pos + new Vector2f(-halfLength, 0)
          }
          val diff = (playerPos - cornerPos)
          if (diff.length() < playerRadius) {
            diff.normalize()
            diff *= playerRadius
            Vector2f.set(cornerPos + diff, playerPos)
          }
        }
      }
    }

    for (wall <- map.cbWalls) {
      val pos = wall.position
      val length = wall.length
      val halfLength = wall.halfLength
      if (Math.abs(pos.y - playerPos.y) < playerRadius && Math.abs(pos.x - playerPos.x) < (playerRadius + halfLength)) { // AABB test
        if (Math.abs(pos.x - playerPos.x) < halfLength) { // front contact
          playerPos.y = pos.y - playerRadius
        } else { // contact on the corner
          val cornerPos = if (playerPos.x > pos.x) { // Right corner
            pos + new Vector2f(halfLength, 0)
          } else { // Left corner
            pos + new Vector2f(-halfLength, 0)
          }
          val diff = (playerPos - cornerPos)
          if (diff.length() < playerRadius) {
            diff.normalize()
            diff *= playerRadius
            Vector2f.set(cornerPos + diff, playerPos)
          }
        }
      }
    }

    for (wall <- map.clWalls) {
      val pos = wall.position
      val length = wall.length
      val halfLength = wall.halfLength
      if (Math.abs(pos.x - playerPos.x) < playerRadius && Math.abs(pos.y - playerPos.y) < (playerRadius + halfLength)) { // AABB test
        if (Math.abs(pos.y - playerPos.y) < halfLength) { // front contact
          playerPos.x = pos.x + playerRadius
        } else { // contact on the corner
          val cornerPos = if (playerPos.y > pos.y) { // down corner
            pos + new Vector2f(0, halfLength)
          } else { // up corner
            pos + new Vector2f(0, -halfLength)
          }
          val diff = (playerPos - cornerPos)
          if (diff.length() < playerRadius) {
            diff.normalize()
            diff *= playerRadius
            Vector2f.set(cornerPos + diff, playerPos)
          }
        }
      }
    }

    for (wall <- map.crWalls) {
      val pos = wall.position
      val length = wall.length
      val halfLength = wall.halfLength
      if (Math.abs(pos.x - playerPos.x) < playerRadius && Math.abs(pos.y - playerPos.y) < (playerRadius + halfLength)) { // AABB test
        if (Math.abs(pos.y - playerPos.y) < halfLength) { // front contact
          playerPos.x = pos.x - playerRadius
        } else { // contact on the corner
          val cornerPos = if (playerPos.y > pos.y) { // down corner
            pos + new Vector2f(0, halfLength)
          } else { // up corner
            pos + new Vector2f(0, -halfLength)
          }
          val diff = (playerPos - cornerPos)
          if (diff.length() < playerRadius) {
            diff.normalize()
            diff *= playerRadius
            Vector2f.set(cornerPos + diff, playerPos)
          }
        }
      }
    }
  }
}
