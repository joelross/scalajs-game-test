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

case object Disconnected
case object RoomJoined

case class RegisterPlayer(playerData: PlayerData)
case class RemovePlayer(player: Player)

case object RoomFull
case class PlayerRegistered(actor: ActorRef)
case object RoomLaunched

case object PingReminder
case object SendPing

class PlayerData(val worker: ServiceWorker, val sendFun: String => Unit)

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

  def registerPlayer(playerData: PlayerData): Unit = this.synchronized {
    implicit val timeout = Timeout(5.seconds)

    def tryRegister(): Unit = {
      val playerRegistered = currentRoom ? RegisterPlayer(playerData)
      playerRegistered.onSuccess {
        case PlayerRegistered =>

        case RoomFull =>
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
  val players: mutable.Set[ActorRef] = mutable.Set[ActorRef]()

  private var nextPlayerId = 1
  private var reportedFull = false

  private val pingIntervalMs = 10000

  private val pingScheduler = this.context.system.scheduler.schedule(pingIntervalMs milliseconds, pingIntervalMs milliseconds, this.self, PingReminder)

  def receive: Receive = {
    case RegisterPlayer(playerData) =>
      if (players.size >= 2 || reportedFull) {
        reportedFull = true
        sender ! RoomFull
      } else {
        val newPlayerId = nextPlayerId
        nextPlayerId += 1

        val player = context.actorOf(Props(classOf[Player], playerData, newPlayerId, this), name = "room" + id + "player" + newPlayerId)

        playerData.worker.playerActor = Some(player)
        players += player

        sender ! PlayerRegistered(player)

        println("Player " + newPlayerId + " connected to room " + id)
      }

    case RemovePlayer(player) =>
      println("Player " + player.id + " disconnected from room " + id)

      players -= player.self
      if (reportedFull && players.size == 0) {
        // This room will not receive further players, let's kill it
        pingScheduler.cancel()
        context.stop(self)
        println("Closing room " + id)
      }

    case PingReminder =>
      players.foreach { player => player ! SendPing }
  }
}

class Player(playerData: PlayerData, val id: Int, room: Room) extends Actor {
  send(demo.Hello(id, demo.Vector3(0, 0, 0), demo.Vector3(0, 0, 0)))

  var data: Option[demo.ClientUpdate] = None

  private var lastPingTime: Option[Long] = None
  private var latency: Option[Int] = None

  def send(msg: demo.ServerMessage): Unit = {
    val data = upickle.write(msg)
    playerData.sendFun(data)
  }

  def receive: Receive = {
    // Client Message
    case demo.Pong => // client's response
      for (time <- lastPingTime) {
        val elapsed = (System.currentTimeMillis() - time) / 2
        println("Latency of " + self.path.name + " is " + elapsed + " ms")
        latency = Some(elapsed.toInt)

        lastPingTime = None
      }
    case demo.KeepAlive       => // nothing to do
    case x: demo.ClientUpdate => data = Some(x) // update local data of the player
    case x: demo.BulletShot   => // handle data

    // Local message
    case Disconnected =>
      room.self ! RemovePlayer(this)
    //context.stop(self) // Done by the parent Room?
    case SendPing =>
      lastPingTime = Some(System.currentTimeMillis())
      send(demo.Ping)
  }
}

class ServiceWorker(val serverConnection: ActorRef) extends HttpServiceActor with websocket.WebSocketServerWorker {
  override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

  var playerActor: Option[ActorRef] = None

  private def sendMessage(msg: String): Unit = send(TextFrame(msg))

  def businessLogic: Receive = {
    case tf: TextFrame => playerActor match {
      case Some(actor) =>
        val payload = tf.payload
        val text = payload.utf8String
        if (!text.isEmpty()) {
          val msg = upickle.read[demo.ClientMessage](text)
          actor ! msg
        }

      case None => println("Warning: TextFrame received from a non-upgraded connection")
    }
    case x: FrameCommandFailed =>
      log.error("frame command failed", x)

    case UpgradedToWebSocket =>
      val playerData = new PlayerData(this, sendMessage _)
      GlobalLogic.registerPlayer(playerData)

    case x: Http.ConnectionClosed => for (actor <- playerActor) {
      actor ! Disconnected
      playerActor = None
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
      val conn = context.actorOf(Props(classOf[ServiceWorker], curSender))
      curSender ! Http.Register(conn)
  }
}

