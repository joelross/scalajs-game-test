package games.demo.server

import games.demo

import scala.concurrent.ExecutionContext.Implicits.global

import akka.pattern.ask
import akka.actor.ActorRef
import akka.actor.Actor
import scala.concurrent.duration._
import akka.util.Timeout
import spray.routing._
import spray.http._
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

import scala.concurrent.duration._

class PlayerInitData(val worker: ConnectionWorker, val sendFun: String => Unit)

sealed trait LocalMessage

// Room messages
sealed trait ToRoomMessage
case class RegisterPlayer(playerActor: ConnectionWorker) extends ToRoomMessage // request to register the player (expect responses)
case class RemovePlayer(player: Player) extends ToRoomMessage // request to remove the player
case object PingReminder extends ToRoomMessage // Room should ping its players

// Room responses to RegisterPlayer
case object RoomFull extends LocalMessage // The room is full and can not accept more players
case object RoomJoined extends LocalMessage // The room has accepted the player

// Player messages
sealed trait ToPlayerMessage
case object SendPing extends ToPlayerMessage // Request a ping to the client
case object Disconnected extends ToPlayerMessage // Signal that the client has disconnected

object GlobalLogic {
  var players: Set[Player] = Set[Player]()

  private var nextRoomId = 0
  private val system = akka.actor.ActorSystem("GlobalLogic")

  private var currentRoom = newRoom()

  private def newRoom() = {
    val actor = system.actorOf(Props(classOf[Room], nextRoomId), name = "room" + nextRoomId)
    nextRoomId += 1
    actor
  }

  def registerPlayer(playerActor: ConnectionWorker): Unit = this.synchronized {
    implicit val timeout = Timeout(5.seconds)

    def tryRegister(): Unit = {
      val playerRegistered = currentRoom ? RegisterPlayer(playerActor)
      playerRegistered.onSuccess {
        case RoomJoined => // Ok, nothing to do

        case RoomFull => // Room rejected the player, create a new room and try again
          currentRoom = newRoom()
          tryRegister()
      }
    }

    tryRegister()
  }

  def renewRoom(): Unit = this.synchronized {
    currentRoom = newRoom()
  }
}

class Room(val id: Int) extends Actor {
  println("Creating room " + id)
  val players: mutable.Set[Player] = mutable.Set[Player]()

  private var nextPlayerId = 1
  private var reportedFull = false

  private val pingIntervalMs = 10000

  private val pingScheduler = this.context.system.scheduler.schedule(pingIntervalMs.milliseconds, pingIntervalMs.milliseconds, this.self, PingReminder)

  def receive: Receive = {
    case RegisterPlayer(playerActor) =>
      if (players.size >= 2 || reportedFull) {
        reportedFull = true
        sender ! RoomFull
      } else {
        val newPlayerId = nextPlayerId
        nextPlayerId += 1

        val player = new Player(playerActor, newPlayerId, this)

        players += player

        sender ! RoomJoined

        println("Player " + newPlayerId + " connected to room " + id)
      }

    case RemovePlayer(player) =>
      println("Player " + player.id + " disconnected from room " + id)

      players -= player
      if (reportedFull && players.size == 0) {
        // This room will not receive further players, let's kill it
        pingScheduler.cancel()
        context.stop(self)
        println("Closing room " + id)
      }

    case PingReminder =>
      players.foreach { player => player.actor.self ! SendPing }
  }
}

class Player(val actor: ConnectionWorker, val id: Int, val room: Room) {
  actor.playerLogic = Some(this)

  sendToClient(demo.Hello(id, demo.Vector3(0, 0, 0), demo.Vector3(0, 0, 0)))

  var data: Option[demo.ClientUpdate] = None

  private var lastPingTime: Option[Long] = None
  private var latency: Option[Int] = None

  def sendToClient(msg: demo.ServerMessage): Unit = {
    val data = upickle.write(msg)
    actor.sendString(data)
  }

  def handleLocalMessage(msg: ToPlayerMessage): Unit = msg match {
    case Disconnected =>
      room.self ! RemovePlayer(this)
    //context.stop(self) // Done by the server at connection's termination?

    case SendPing =>
      lastPingTime = Some(System.currentTimeMillis())
      sendToClient(demo.Ping)
  }

  def handleClientMessage(msg: demo.ClientMessage): Unit = msg match {
    case demo.Pong => // client's response
      for (time <- lastPingTime) {
        val elapsed = (System.currentTimeMillis() - time) / 2
        println("Latency of player " + id + " in room " + room.id + " is " + elapsed + " ms")
        latency = Some(elapsed.toInt)
        lastPingTime = None
      }
    case demo.KeepAlive       => // nothing to do
    case x: demo.ClientUpdate => data = Some(x) // update local data of the player
    case x: demo.BulletShot   => // handle data
  }
}

class ConnectionWorker(val serverConnection: ActorRef) extends HttpServiceActor with websocket.WebSocketServerWorker {
  override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

  var playerLogic: Option[Player] = None

  def sendString(msg: String): Unit = send(TextFrame(msg))

  def businessLogic: Receive = {
    case tf: TextFrame => playerLogic match {
      case Some(logic) =>
        val payload = tf.payload
        val text = payload.utf8String
        if (!text.isEmpty()) {
          val clientMsg = upickle.read[demo.ClientMessage](text)
          logic.handleClientMessage(clientMsg)
        }
      case None => println("Warning: connection not yet upgraded to player; can not process client message")
    }
    case x: FrameCommandFailed =>
      log.error("frame command failed", x)

    case UpgradedToWebSocket =>
      GlobalLogic.registerPlayer(this)

    case x: Http.ConnectionClosed => for (logic <- playerLogic) {
      logic.handleLocalMessage(Disconnected)
      playerLogic = None
    }

    case localMsg: ToPlayerMessage => playerLogic match {
      case Some(logic) => logic.handleLocalMessage(localMsg)
      case None        => println("Warning: connection not yet upgraded to player; can not process local message")
    }
  }

  def businessLogicNoUpgrade: Receive = {
    implicit val refFactory: ActorRefFactory = context
    runRoute(myRoute)
  }

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
      val conn = context.actorOf(Props(classOf[ConnectionWorker], curSender))
      curSender ! Http.Register(conn)
  }
}

