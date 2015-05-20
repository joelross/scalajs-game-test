package games.demo.network

case class Vector2(x: Float, y: Float)
case class Vector3(x: Float, y: Float, z: Float)

case class ProjectileIdentifier(playerId: Int, projectileId: Int)

sealed trait State
case object Absent extends State
case class Playing(position: Vector2, velocity: Vector2, orientation: Float) extends State

case class PlayerData(id: Int, latency: Int, state: State)

sealed trait Event
case class ProjectileCreation(id: ProjectileIdentifier, position: Vector2, orientation: Float) extends Event
case class ProjectileDestruction(id: ProjectileIdentifier, playerHit: Int) extends Event

sealed trait NetworkMessage
sealed trait ClientMessage extends NetworkMessage
sealed trait ServerMessage extends NetworkMessage
// Server -> Client
case object ServerPing extends ServerMessage
case class ServerHello(playerId: Int) extends ServerMessage
case class ServerUpdate(players: Seq[PlayerData], newEvents: Seq[Event]) extends ServerMessage
// Server <- Client
case object ClientPong extends ClientMessage
case class ClientPositionUpdate(state: State) extends ClientMessage
case class ClientProjectileShot(id: Int, position: Vector2, orientation: Float) extends ClientMessage
case class ClientProjectileHit(id: Int, playerHitId: Int) extends ClientMessage
