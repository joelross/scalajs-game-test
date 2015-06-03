package games.demo

import games.math._

import scala.collection.{ immutable, mutable }

object Physics {
  final val playerRadius = 0.5f
  final val projectileVelocity = 15f

  /**
   * Sets an angle in degrees in the interval ]-180, 180]
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

  def projectileStep(proj: (Int, Projectile), players: immutable.Map[Int, Present], elapsedSinceLastFrame: Float): Int = {
    val (shooterId, projectile) = proj

    // Move the projectile
    val direction = Matrix2f.rotate2D(-projectile.orientation) * new Vector2f(0, -1)
    val distance = projectileVelocity * elapsedSinceLastFrame

    val startPoint = projectile.position

    // Collision detection

    // players
    val playerRes = players.toSeq.flatMap { p =>
      val (playerId, player) = p
      if (shooterId != playerId) { // No self-hit...
        // From http://mathworld.wolfram.com/Circle-LineIntersection.html

        val x1 = startPoint.x - player.position.x
        val y1 = startPoint.y - player.position.y

        val r = playerRadius
        val r_square = r * r

        if ((x1 * x1 + y1 * y1) <= r_square) { // Already in contact
          Some((playerId, 0f))
        } else {
          val dx = direction.x
          val dy = direction.y

          val x2 = x1 + dx
          val y2 = y1 + dy

          // dr is always 1 (dx and dy are part of a unit vector)
          val d = x1 * y2 - x2 * y1

          val disc = r_square - d * d

          if (disc >= 0f) {
            // I know Math.signum looks the same, but we need sgn(0) to return 1 in this case
            def sgn(in: Float): Float = if (in < 0f) -1f else 1f

            val disc_sqrt = Math.sqrt(disc).toFloat

            val partx = sgn(dy) * dx * disc_sqrt
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
              Some((playerId, collision_distance))
            } else None
          } else None
        }
      } else None
    }

    // Map
    val hWalls = map.ctWalls ++ map.cbWalls
    val vWalls = map.crWalls ++ map.clWalls

    val hRes = hWalls.flatMap { hWall =>
      val dx = direction.x
      val dy = direction.y

      val wx = hWall.position.x
      val wy = hWall.position.y

      val x4 = startPoint.x
      val y4 = startPoint.y

      val x3 = x4 + dx
      val y3 = y4 + dy

      if (dy == 0f) None // Parallel to the wall, no contact
      else {
        val x = (wy * dx - x3 * y4 + x4 * y3) / dy
        val y = wy

        val l = (x - x4) * dx + (y - y4) * dy
        val l_valid = (l >= 0f && l <= distance) && Math.abs(wx - x) < hWall.halfLength

        if (l_valid) Some((0, l))
        else None
      }
    }

    val vRes = vWalls.flatMap { vWall =>
      val dx = direction.x
      val dy = direction.y

      val wx = vWall.position.x
      val wy = vWall.position.y

      val x4 = startPoint.x
      val y4 = startPoint.y

      val x3 = x4 + dx
      val y3 = y4 + dy

      if (dx == 0f) None // Parallel to the wall, no contact
      else {
        val y = (wx * dy - y3 * x4 + y4 * x3) / dx
        val x = wx

        val l = (x - x4) * dx + (y - y4) * dy
        val l_valid = (l >= 0f && l <= distance) && Math.abs(wy - y) < vWall.halfLength

        if (l_valid) Some((0, l))
        else None
      }
    }

    val res = playerRes ++ hRes ++ vRes

    val (playerId, distance_travel) = if (res.isEmpty) (-1, distance) // No collision
    else res.reduce { (a1, a2) => // Collision(s) detected, take the closest one
      val (p1, d1) = a1
      val (p2, d2) = a2

      if (d1 < d2) a1
      else a2
    }

    projectile.position = projectile.position + direction * distance_travel // new position
    playerId // -1 no collision, 0 wall, > 0 player hit
  }

  def playerStep(player: Present, elapsedSinceLastFrame: Float): Unit = {
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
