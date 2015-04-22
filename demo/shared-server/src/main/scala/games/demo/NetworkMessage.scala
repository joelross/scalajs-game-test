package games.demo

case class Vector3(x: Float, y: Float, z: Float)

case class SpaceData(position: Vector3, velocity: Float, orientation: Vector3, rotation: Vector3)

case class ServerUpdatePlayerData(id: Int, latency: Int, space: Option[SpaceData])
sealed trait Event
case class BulletCreation(id: Int, playerId: Int, initialPosition: Vector3, orientation: Vector3) extends Event
case class BulletDestruction(bulletId: Int, playerHitId: Int) extends Event

sealed trait NetworkMessage
sealed trait ClientMessage extends NetworkMessage
sealed trait ServerMessage extends NetworkMessage
// Server -> Client
case object Ping extends ServerMessage
case class Hello(playerId: Int, initPos: Vector3, initDir: Vector3) extends ServerMessage
case class ServerUpdate(players: Seq[ServerUpdatePlayerData], newEvents: Seq[Event]) extends ServerMessage
// Server <- Client
case object Pong extends ClientMessage
case class ClientPositionUpdate(position: Vector3, velocity: Float, orientation: Vector3, rotation: Vector3) extends ClientMessage
case class BulletShot(initialPosition: Vector3, orientation: Vector3) extends ClientMessage
case class BulletHit(bulletId: Int, playerHitId: Int) extends ClientMessage
case class Message(msg: String) extends ClientMessage // TODO for testing