package games.demo

import games._
import games.math.{ Vector2f, Vector3f }
import scala.concurrent.{ Future, ExecutionContext }

import scala.collection.immutable
import scala.collection.mutable

object Map {
  val roomSize: Float = 2f
  val roomHalfSize: Float = roomSize / 2

  def load(resourceMap: Resource)(implicit ec: ExecutionContext): Future[Map] = {
    val mapFileFuture: Future[String] = Utils.getTextDataFromResource(resourceMap)

    for (mapFile <- mapFileFuture) yield {
      val lines: Array[String] = Utils.lines(mapFile)

      val rooms: mutable.ArrayBuffer[Room] = mutable.ArrayBuffer()
      val starts: mutable.Map[Int, Room] = mutable.Map()

      var y: Int = 0
      for (line <- lines) {
        var x: Int = 0
        for (char <- line) {

          char match {
            case 'x' => rooms += Room(x, y)

            case v if Character.isDigit(v) =>
              val number = v - '0'
              val room = Room(x, y)
              rooms += room
              starts += (number -> room)

            case _ =>
          }

          x += 1
        }
        y += 1
      }

      val width = rooms.map(_.x).reduce(Math.max) + 1
      val height = rooms.map(_.y).reduce(Math.max) + 1

      val array = Array.ofDim[Option[Room]](width, height)

      for (
        x <- 0 until width;
        y <- 0 until height
      ) {
        array(x)(y) = rooms.find { r => r.x == x && r.y == y }
      }

      Map(array, starts.toMap, width, height)
    }
  }
}

case class Room(x: Int, y: Int) {
  lazy val center = new Vector2f(Map.roomSize * x + Map.roomHalfSize, Map.roomSize * y + Map.roomHalfSize)
}

case class Map(rooms: Array[Array[Option[Room]]], starts: immutable.Map[Int, Room], width: Int, height: Int) {
  def roomAt(x: Int, y: Int): Option[Room] = if (x >= 0 && x < width && y >= 0 && y < height) rooms(x)(y) else None

  val (floors, lWalls, rWalls, tWalls, bWalls) = {
    val floors: mutable.ArrayBuffer[Vector2f] = mutable.ArrayBuffer()
    val lWalls: mutable.ArrayBuffer[Vector2f] = mutable.ArrayBuffer() // Left walls
    val rWalls: mutable.ArrayBuffer[Vector2f] = mutable.ArrayBuffer() // Right walls
    val tWalls: mutable.ArrayBuffer[Vector2f] = mutable.ArrayBuffer() // Top walls
    val bWalls: mutable.ArrayBuffer[Vector2f] = mutable.ArrayBuffer() // Bottom walls

    for (
      x <- 0 until width;
      y <- 0 until height
    ) {
      rooms(x)(y) match {
        case Some(room) =>
          floors += new Vector2f(Map.roomSize * room.x + Map.roomHalfSize, Map.roomSize * room.y + Map.roomHalfSize)
          if (!roomAt(room.x - 1, room.y).isDefined) lWalls += new Vector2f(Map.roomSize * room.x, Map.roomSize * room.y + Map.roomHalfSize)
          if (!roomAt(room.x + 1, room.y).isDefined) rWalls += new Vector2f(Map.roomSize * (room.x + 1), Map.roomSize * room.y + Map.roomHalfSize)
          if (!roomAt(room.x, room.y - 1).isDefined) tWalls += new Vector2f(Map.roomSize * room.x + Map.roomHalfSize, Map.roomSize * room.y)
          if (!roomAt(room.x, room.y + 1).isDefined) bWalls += new Vector2f(Map.roomSize * room.x + Map.roomHalfSize, Map.roomSize * (room.y + 1))

        case None =>
      }
    }

    (floors.toArray, lWalls.toArray, rWalls.toArray, tWalls.toArray, bWalls.toArray)
  }
}