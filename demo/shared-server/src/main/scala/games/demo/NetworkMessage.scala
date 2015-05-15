package games.demo

case class Vector2(x: Float, y: Float)
case class Vector3(x: Float, y: Float, z: Float)

case class SpaceData(position: Vector2, orientation: Float)
case class MoveData(space: SpaceData, velocity: Vector2)

case class ProjectileIdentifier(playerId: Int, projectileId: Int)

case class ServerUpdatePlayerData(id: Int, latency: Int, move: Option[MoveData])
sealed trait Event
case class ProjectileCreation(id: ProjectileIdentifier, position: Vector2, orientation: Float) extends Event
case class ProjectileDestruction(id: ProjectileIdentifier) extends Event
case class PlayerHit(id: Int) extends Event

sealed trait NetworkMessage
sealed trait ClientMessage extends NetworkMessage
sealed trait ServerMessage extends NetworkMessage
// Server -> Client
case object Ping extends ServerMessage
case class Hello(playerId: Int) extends ServerMessage
case class SetPosition(space: SpaceData)
case class ServerUpdate(players: Seq[ServerUpdatePlayerData], newEvents: Seq[Event]) extends ServerMessage
// Server <- Client
case object Pong extends ClientMessage
case class ClientPositionUpdate(move: MoveData) extends ClientMessage
case class ProjectileShot(id: Int, position: Vector2, orientation: Float) extends ClientMessage
case class ProjectileHit(id: Int, playerHitId: Int) extends ClientMessage
