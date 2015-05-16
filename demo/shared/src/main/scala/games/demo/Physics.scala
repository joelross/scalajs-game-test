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
    val diff = direction * distance

    val startPoint = projectile.position
    val endPoint = projectile.position + diff

    projectile.position = endPoint

    // Collision detection
    val res = players.flatMap {
      case (playerId, player) =>
        if (shooterId != playerId) {
          val x1 = startPoint.x - player.position.x
          val y1 = startPoint.y - player.position.y
          val x2 = endPoint.x - player.position.x
          val y2 = endPoint.y - player.position.y

          val dx = x2 - x1
          val dy = y2 - y1

          val r = playerRadius
          val dr_square = (dx * dx + dy * dy)
          val d = x1 * y2 - x2 * y1

          val disc = r * r * dr_square - d * d

          if (disc >= 0f) {
            val disc_sqrt = Math.sqrt(r * r * dr_square - d * d).toFloat
            val cx1 = (d * dy + Math.signum(dy) * dx * disc_sqrt) / dr_square
            val cy1 = (-d * dx + Math.abs(dy) * disc_sqrt) / dr_square
            // val c1 = new Vector2f(cx1 + player.position.x, cy1 + player.position.y)

            val cx2 = (d * dy - Math.signum(dy) * dx * disc_sqrt) / dr_square
            val cy2 = (-d * dx - Math.abs(dy) * disc_sqrt) / dr_square
            // val c2 = new Vector2f(cx2 + player.position.x, cy2 + player.position.y)

            // val l1 = (c1 - startPoint).length()
            // val l2 = (c2 - startPoint).length()
            val p1 = (cx1 + player.position.x) - startPoint.x
            val q1 = (cy1 + player.position.y) - startPoint.y

            val p2 = (cx2 + player.position.x) - startPoint.x
            val q2 = (cy2 + player.position.y) - startPoint.y

            val l1 = Math.sqrt(p1 * p1 + q1 * q1).toFloat
            val l2 = Math.sqrt(p2 * p2 + q2 * q2).toFloat

            val l = Math.min(l1, l2)

            if (l < distance) Some(playerId)
            else None
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
