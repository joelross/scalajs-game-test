package games.demo

case class Vector3(x: Float, y: Float, z: Float)

case class SpaceData(position: Vector3, velocity: Float, orientation: Vector3, rotation: Vector3)

case class PlayerServerUpdate(id: Int, latency: Int, space: Option[SpaceData])
sealed trait Event
case class BulletCreation(id: Int, playerId: Int, initialPosition: Vector3, initialOrientation: Vector3) extends Event
case class BulletHit(playerId: Int, shotId: Int, playerDestroyed: Boolean) extends Event

sealed trait NetworkMessage
sealed trait ClientMessage extends NetworkMessage
sealed trait ServerMessage extends NetworkMessage
// Server -> Client
case object Ping extends ServerMessage
case class Hello(playerId: Int, initPos: Vector3, initDir: Vector3) extends ServerMessage
case class ServerUpdate(players: Seq[PlayerServerUpdate], newEvents: Seq[Event]) extends ServerMessage
// Server <- Client
case object Pong extends ClientMessage
case class ClientUpdate(position: Vector3, velocity: Float, orientation: Vector3, rotation: Vector3) extends ClientMessage
case class BulletShot(initialPosition: Vector3, direction: Vector3) extends ClientMessage