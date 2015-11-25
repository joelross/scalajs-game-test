package games.demo.server

import games.demo.network
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, Promise, ExecutionContext }
import akka.pattern.ask
import akka.actor.ActorRef
import akka.actor.Actor
import scala.concurrent.duration._
import akka.util.Timeout
import spray.routing._
import spray.http._
import spray.http.CacheDirectives._
import spray.http.HttpHeaders._
import MediaTypes._
import spray.can.websocket
import spray.can.websocket.frame.{ Frame, TextFrame, BinaryFrame }
import spray.can.websocket.FrameCommandFailed
import spray.can.websocket.UpgradedToWebSocket
import spray.can.websocket.FrameCommand
import akka.actor.ActorRefFactory
import spray.can.Http
import akka.actor.Props
import scala.collection.mutable
import scala.collection.immutable
import scala.concurrent.duration._
import java.util.concurrent.Semaphore
import scala.concurrent.Await

sealed trait LocalMessage

// Room messages
sealed trait ToRoomMessage
case class RegisterPlayer(playerActor: ConnectionActor) extends ToRoomMessage // request to register the player (expect responses)
case class RemovePlayer(player: Player) extends ToRoomMessage // request to remove the player
case object PingReminder extends ToRoomMessage // Room should ping its players
case object UpdateReminder extends ToRoomMessage // Room should update the data of its players

// Room responses to RegisterPlayer
case object RoomFull extends LocalMessage // The room is full and can not accept more players
case object RoomJoined extends LocalMessage // The room has accepted the player

// Player messages
sealed trait ToPlayerMessage
case object SendPing extends ToPlayerMessage // Request a ping sent to the client
case object Disconnected extends ToPlayerMessage // Signal that the client has disconnected

// Player response to GetData
case class PlayerDataResponse(projShots: immutable.Seq[network.ClientProjectileShot], projHits: immutable.Seq[network.ClientProjectileHit], data: network.PlayerData)

object GlobalLogic {
  var players: Set[Player] = Set[Player]()

  private val lock = new Semaphore(1)

  private var nextRoomId = 0
  private val system = akka.actor.ActorSystem("GlobalLogic")

  private var currentRoom = newRoom()

  private def newRoom() = {
    val actor = system.actorOf(Props(classOf[Room], nextRoomId), name = "room" + nextRoomId)
    nextRoomId += 1
    actor
  }

  def registerPlayer(playerActor: ConnectionActor): Unit = {
    lock.acquire()
    implicit val timeout = Timeout(5.seconds)

    def tryRegister(): Unit = {
      val playerRegistered = currentRoom ? RegisterPlayer(playerActor)
      playerRegistered.onSuccess {
        case RoomJoined => // Ok, nothing to do
          lock.release()

        case RoomFull => // Room rejected the player, create a new room and try again
          currentRoom = newRoom()
          tryRegister()
      }
      playerRegistered.onFailure {
        case _ =>
          lock.release()
      }
    }

    tryRegister()
  }
}

class Room(val id: Int) extends Actor {
  println("Creating room " + id)

  val maxPlayers = 8

  val players: mutable.Set[Player] = mutable.Set()

  private def nextPlayerId(): Int = {
    def tryFrom(v: Int): Int = {
      if (players.forall { p => p.id != v }) v
      else tryFrom(v + 1)
    }

    tryFrom(1)
  }

  private var reportedFull = false

  private val pingIntervalMs = 5000 // Once every 5 seconds
  private val pingScheduler = this.context.system.scheduler.schedule(pingIntervalMs.milliseconds, pingIntervalMs.milliseconds, this.self, PingReminder)

  private val updateIntervalMs = 50 // about 20Hz refresh rate
  private val updateScheduler = this.context.system.scheduler.schedule(updateIntervalMs.milliseconds, updateIntervalMs.milliseconds, this.self, UpdateReminder)

  def receive: Receive = {
    case RegisterPlayer(playerActor) =>
      if (players.size >= maxPlayers || reportedFull) {
        reportedFull = true
        sender ! RoomFull
      } else {
        val newPlayerId = nextPlayerId()
        val player = new Player(playerActor, newPlayerId, this)

        players += player

        sender ! RoomJoined

        println("Player " + newPlayerId + " connected to room " + id)
      }

    case RemovePlayer(player) =>
      players -= player
      println("Player " + player.id + " disconnected from room " + id)
      if (players.isEmpty && reportedFull) {
        // This room is empty and will not receive further players, let's kill it
        pingScheduler.cancel()
        updateScheduler.cancel()
        context.stop(self)
        println("Closing room " + id)
      }

    case PingReminder =>
      players.foreach { player => player.actor.self ! SendPing }

    case UpdateReminder =>
      val playersResponse = players.map { player => player.getData() }

      val playersMsgData = playersResponse.map { response => response.data }.toSeq

      val projectileShotsData = (for (playerResponse <- playersResponse; projShot <- playerResponse.projShots) yield {
        val projId = network.ProjectileIdentifier(playerResponse.data.id, projShot.id)
        network.ProjectileShot(projId, projShot.position, projShot.orientation)
      }).toSeq

      val projectileHitsData = (for (playerResponse <- playersResponse; projHit <- playerResponse.projHits) yield {
        val projId = network.ProjectileIdentifier(playerResponse.data.id, projHit.id)
        network.ProjectileHit(projId, projHit.playerHitId)
      }).toSeq

      val events = immutable.Seq() ++ projectileShotsData ++ projectileHitsData
      val updateMsg = network.ServerUpdate(playersMsgData, events)

      players.foreach { player =>
        player.sendToClient(updateMsg)
      }
  }
}

class Player(val actor: ConnectionActor, val id: Int, val room: Room) {
  // Init
  actor.playerLogic = Some(this)
  sendToClient(network.ServerHello(id))

  private var lastPingTime: Option[Long] = None

  private var latency: Int = 0
  private var state: network.State = network.Absent
  private val projectileShotsData: mutable.Queue[network.ClientProjectileShot] = mutable.Queue()
  private val projectileHitsData: mutable.Queue[network.ClientProjectileHit] = mutable.Queue()

  def sendToClient(msg: network.ServerMessage): Unit = {
    val data = upickle.default.write(msg)
    actor.sendString(data)
  }

  def getData(): PlayerDataResponse = this.synchronized {
    val ret = PlayerDataResponse(immutable.Seq() ++ projectileShotsData, immutable.Seq() ++ projectileHitsData, network.PlayerData(this.id, this.latency, this.state))
    projectileShotsData.clear()
    projectileHitsData.clear()
    ret
  }

  def handleLocalMessage(msg: ToPlayerMessage): Unit = msg match {
    case Disconnected =>
      room.self ! RemovePlayer(this)
    //context.stop(self) // Done by the server at connection's termination?

    case SendPing =>
      lastPingTime = Some(System.currentTimeMillis())
      sendToClient(network.ServerPing)
  }

  def handleClientMessage(msg: network.ClientMessage): Unit = msg match {
    case network.ClientPong => // client's response
      for (time <- lastPingTime) {
        val elapsed = (System.currentTimeMillis() - time) / 2
        this.synchronized { latency = elapsed.toInt }
        lastPingTime = None
      }
    case x: network.ClientUpdate         => this.synchronized { this.state = x.state }
    case x: network.ClientProjectileShot => this.synchronized { this.projectileShotsData += x }
    case x: network.ClientProjectileHit  => this.synchronized { this.projectileHitsData += x }
  }
}

class ConnectionActor(val serverConnection: ActorRef) extends HttpServiceActor with websocket.WebSocketServerWorker {
  override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

  var playerLogic: Option[Player] = None

  def sendString(msg: String): Unit = send(TextFrame(msg))

  def businessLogic: Receive = {
    case localMsg: ToPlayerMessage => playerLogic match {
      case Some(logic) => logic.handleLocalMessage(localMsg)
      case None        => println("Warning: connection not yet upgraded to player; can not process local message")
    }

    case tf: TextFrame => playerLogic match {
      case Some(logic) =>
        val payload = tf.payload
        val text = payload.utf8String
        if (!text.isEmpty()) {
          val clientMsg = upickle.default.read[network.ClientMessage](text)
          logic.handleClientMessage(clientMsg)
        }
      case None => println("Warning: connection not yet upgraded to player; can not process client message")
    }

    case x: FrameCommandFailed =>
      log.error("frame command failed", x)

    case UpgradedToWebSocket => playerLogic match {
      case None => GlobalLogic.registerPlayer(this)
      case _    => println("Warning: the connection has already been upgraded to player")
    }

    case x: Http.ConnectionClosed => for (logic <- playerLogic) {
      logic.handleLocalMessage(Disconnected)
      playerLogic = None
    }
  }

  def businessLogicNoUpgrade: Receive = {
    implicit val refFactory: ActorRefFactory = context
    runRoute(cachedRoute)
  }

  def cachedRoute = respondWithHeader(`Cache-Control`(`public`, `no-cache`)) { myRoute }

  val myRoute =
    path("") {
      respondWithMediaType(`text/html`) {
        getFromFile("../../demoJS-launcher/index.html")
      }
    } ~
      path("fast") {
        respondWithMediaType(`text/html`) {
          getFromFile("../../demoJS-launcher/index-fastopt.html")
        }
      } ~
      path("code" / Rest) { file =>
        val path = "../js/target/scala-2.11/" + file
        getFromFile(path)
      } ~
      path("resources" / Rest) { file =>
        val path = "../shared/src/main/resources/" + file
        getFromFile(path)
      }
}

class Service extends Actor {

  def receive = {
    case Http.Connected(remoteAddress, localAddress) =>
      val curSender = sender()
      val conn = context.actorOf(Props(classOf[ConnectionActor], curSender))
      curSender ! Http.Register(conn)
  }
}

