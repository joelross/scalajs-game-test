package games.demo

import games._
import games.math.{ Vector2f, Vector3f }
import scala.concurrent.{ Future, ExecutionContext }

import scala.collection.immutable
import scala.collection.mutable

object Map {
  final val roomSize: Float = 2f
  final val roomHalfSize: Float = roomSize / 2

  def coordinates(pos: Vector2f): (Int, Int) = (Math.floor(pos.x / Map.roomSize).toInt, Math.floor(pos.y / Map.roomSize).toInt)

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
            case 'x' => rooms += new Room(x, y)

            case v if Character.isDigit(v) =>
              val number = v - '0'
              val room = new Room(x, y)
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

      new Map(array, starts.toMap, width, height)
    }
  }
}

class Room(val x: Int, val y: Int) {
  lazy val center = new Vector2f(Map.roomSize * x + Map.roomHalfSize, Map.roomSize * y + Map.roomHalfSize)
}

class ContinuousWall(val position: Vector2f, val length: Float) {
  val halfLength = length / 2
}

class Map(val rooms: Array[Array[Option[Room]]], val starts: immutable.Map[Int, Room], val width: Int, val height: Int) {
  def roomAt(x: Int, y: Int): Option[Room] = if (x >= 0 && x < width && y >= 0 && y < height) rooms(x)(y) else None
  def roomAt(pos: Vector2f): Option[Room] = {
    val (x, y) = Map.coordinates(pos)
    roomAt(x, y)
  }

  val definedRooms = for (
    x <- 0 until width;
    y <- 0 until height;
    room <- rooms(x)(y)
  ) yield room

  def hasLWall(room: Room): Boolean = roomAt(room.x - 1, room.y).isEmpty
  def hasRWall(room: Room): Boolean = roomAt(room.x + 1, room.y).isEmpty
  def hasTWall(room: Room): Boolean = roomAt(room.x, room.y - 1).isEmpty
  def hasBWall(room: Room): Boolean = roomAt(room.x, room.y + 1).isEmpty

  val (floors, lWalls, rWalls, tWalls, bWalls) = {
    val floors: mutable.ArrayBuffer[Vector2f] = mutable.ArrayBuffer()
    val lWalls: mutable.ArrayBuffer[Vector2f] = mutable.ArrayBuffer() // Left walls
    val rWalls: mutable.ArrayBuffer[Vector2f] = mutable.ArrayBuffer() // Right walls
    val tWalls: mutable.ArrayBuffer[Vector2f] = mutable.ArrayBuffer() // Top walls
    val bWalls: mutable.ArrayBuffer[Vector2f] = mutable.ArrayBuffer() // Bottom walls

    for (
      room <- definedRooms
    ) {
      floors += new Vector2f(Map.roomSize * room.x + Map.roomHalfSize, Map.roomSize * room.y + Map.roomHalfSize)
      if (hasLWall(room)) lWalls += new Vector2f(Map.roomSize * room.x, Map.roomSize * room.y + Map.roomHalfSize)
      if (hasRWall(room)) rWalls += new Vector2f(Map.roomSize * (room.x + 1), Map.roomSize * room.y + Map.roomHalfSize)
      if (hasTWall(room)) tWalls += new Vector2f(Map.roomSize * room.x + Map.roomHalfSize, Map.roomSize * room.y)
      if (hasBWall(room)) bWalls += new Vector2f(Map.roomSize * room.x + Map.roomHalfSize, Map.roomSize * (room.y + 1))
    }

    (floors.toArray, lWalls.toArray, rWalls.toArray, tWalls.toArray, bWalls.toArray)
  }

  val (clWalls, crWalls, ctWalls, cbWalls) = {
    val lWalls: mutable.ArrayBuffer[ContinuousWall] = mutable.ArrayBuffer() // Left walls
    val rWalls: mutable.ArrayBuffer[ContinuousWall] = mutable.ArrayBuffer() // Right walls
    val tWalls: mutable.ArrayBuffer[ContinuousWall] = mutable.ArrayBuffer() // Top walls
    val bWalls: mutable.ArrayBuffer[ContinuousWall] = mutable.ArrayBuffer() // Bottom walls

    // TODO FIXME too much copy-paste, make this more modular

    for (y <- 0 until height) {
      var startT: Option[Int] = None
      var startB: Option[Int] = None

      def flushT(start: Int, end: Int): Unit = {
        val length = (end - start + 1) * Map.roomSize
        val cenX = start * Map.roomSize + (end - start + 1) * Map.roomHalfSize
        val cenY = Map.roomSize * y
        tWalls += new ContinuousWall(new Vector2f(cenX, cenY), length)
      }
      def flushB(start: Int, end: Int): Unit = {
        val length = (end - start + 1) * Map.roomSize
        val cenX = start * Map.roomSize + (end - start + 1) * Map.roomHalfSize
        val cenY = Map.roomSize * (y + 1)
        bWalls += new ContinuousWall(new Vector2f(cenX, cenY), length)
      }

      for (x <- 0 until width) {
        val hasWallT = roomAt(x, y).map(hasTWall).getOrElse(false)
        (startT, hasWallT) match {
          case (Some(s), true) => // nothing to do, keep going
          case (None, true)    => startT = Some(x) // start of a new wall
          case (Some(s), false) =>
            flushT(s, x - 1)
            startT = None // End of the wall
          case (None, false) => // nothing to do, keep going
        }

        val hasWallB = roomAt(x, y).map(hasBWall).getOrElse(false)
        (startB, hasWallB) match {
          case (Some(s), true) => // nothing to do, keep going
          case (None, true)    => startB = Some(x) // start of a new wall
          case (Some(s), false) =>
            flushB(s, x - 1)
            startB = None // End of the wall
          case (None, false) => // nothing to do, keep going
        }
      }

      for (s <- startT) {
        flushT(s, width - 1)
      }
      for (s <- startB) {
        flushB(s, width - 1)
      }
    }

    for (x <- 0 until width) {
      var startL: Option[Int] = None
      var startR: Option[Int] = None

      def flushL(start: Int, end: Int): Unit = {
        val length = (end - start + 1) * Map.roomSize
        val cenY = start * Map.roomSize + (end - start + 1) * Map.roomHalfSize
        val cenX = Map.roomSize * x
        lWalls += new ContinuousWall(new Vector2f(cenX, cenY), length)
      }
      def flushR(start: Int, end: Int): Unit = {
        val length = (end - start + 1) * Map.roomSize
        val cenY = start * Map.roomSize + (end - start + 1) * Map.roomHalfSize
        val cenX = Map.roomSize * (x + 1)
        rWalls += new ContinuousWall(new Vector2f(cenX, cenY), length)
      }

      for (y <- 0 until height) {
        val hasWallL = roomAt(x, y).map(hasLWall).getOrElse(false)
        (startL, hasWallL) match {
          case (Some(s), true) => // nothing to do, keep going
          case (None, true)    => startL = Some(y) // start of a new wall
          case (Some(s), false) =>
            flushL(s, y - 1)
            startL = None // End of the wall
          case (None, false) => // nothing to do, keep going
        }

        val hasWallR = roomAt(x, y).map(hasRWall).getOrElse(false)
        (startR, hasWallR) match {
          case (Some(s), true) => // nothing to do, keep going
          case (None, true)    => startR = Some(y) // start of a new wall
          case (Some(s), false) =>
            flushR(s, y - 1)
            startR = None // End of the wall
          case (None, false) => // nothing to do, keep going
        }
      }

      for (s <- startL) {
        flushL(s, height - 1)
      }
      for (s <- startR) {
        flushR(s, height - 1)
      }
    }

    (lWalls.toArray, rWalls.toArray, tWalls.toArray, bWalls.toArray)
  }
}