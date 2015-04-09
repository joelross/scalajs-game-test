package games.demo

case class Vector3(x: Float, y: Float, z: Float)

case class PlayerServerUpdate(id: Int, position: Vector3, velocity: Vector3, orientation: Vector3, rotation: Vector3)
sealed trait Event
case class BulletCreation(id: Int, playerId: Int, initialPosition: Vector3, direction: Vector3) extends Event
case class BulletHit(playerId: Int, shotId: Int, playerDestroyed: Boolean) extends Event

sealed trait NetworkMessage
// Server <> Client
case class KeepAlive() extends NetworkMessage
// Server -> Client
case class Hello(playerId: Int) extends NetworkMessage
case class ServerUpdate(players: Seq[PlayerServerUpdate], newEvents: Seq[Event]) extends NetworkMessage
// Server <- Client
case class ClientUpdate(position: Vector3, velocity: Vector3, orientation: Vector3, rotation: Vector3) extends NetworkMessage