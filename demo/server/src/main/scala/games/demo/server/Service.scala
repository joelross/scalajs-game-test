package games.demo.server

import games.demo
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, Promise, ExecutionContext }
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
case object AskData extends ToPlayerMessage // request data of the player (expect responses)

// Player response to GetData
case class DataResponse(projShots: immutable.Seq[demo.ProjectileShot], projHits: immutable.Seq[demo.ProjectileHit], data: demo.ServerUpdatePlayerData) extends LocalMessage

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

  val maxPlayers = 2

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

  private val updateIntervalMs = 100 // 10Hz refresh rate
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
      implicit val timeout = Timeout(100.milliseconds)

      val playersData = players.map { player =>
        (player.actor.self ? AskData).mapTo[DataResponse]
      }

      val allFuture = Future.sequence(playersData)

      val all = Await.result(allFuture, Duration.Inf)

      val playersMsgData = (for (playerResponse <- all) yield {
        playerResponse.data
      }).toSeq

      val bulletShotsData = (for (playerResponse <- all; projShot <- playerResponse.projShots) yield {
        val projId = demo.ProjectileIdentifier(playerResponse.data.id, projShot.id)
        val projCreation = demo.ProjectileCreation(projId, projShot.position, projShot.orientation)
        projCreation.asInstanceOf[demo.Event]
      }).toSeq

      val bulletHitsData = (for (playerResponse <- all; projHit <- playerResponse.projHits) yield {
        val projId = demo.ProjectileIdentifier(playerResponse.data.id, projHit.id)
        val projDestruction = demo.ProjectileDestruction(projId)
        projDestruction.asInstanceOf[demo.Event]
      }).toSeq

      val events = immutable.Seq() ++ bulletShotsData ++ bulletHitsData
      val updateMsg = demo.ServerUpdate(playersMsgData, events)
      players.foreach { player =>
        player.sendToClient(updateMsg)
      }
  }
}

class Player(val actor: ConnectionActor, val id: Int, val room: Room) {
  // Init
  actor.playerLogic = Some(this)
  sendToClient(demo.Hello(id))

  private var lastPingTime: Option[Long] = None

  var latency: Option[Int] = None

  var updateData: Option[demo.ClientPositionUpdate] = None
  val bulletShotsData: mutable.Queue[demo.ProjectileShot] = mutable.Queue()
  val bulletHitsData: mutable.Queue[demo.ProjectileHit] = mutable.Queue()

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

    case AskData =>
      actor.sender ! DataResponse(immutable.Seq() ++ bulletShotsData, immutable.Seq() ++ bulletHitsData, demo.ServerUpdatePlayerData(this.id, this.latency.getOrElse(0), updateData.map { data => data.move }))
      bulletShotsData.clear()
      bulletHitsData.clear()
  }

  def handleClientMessage(msg: demo.ClientMessage): Unit = msg match {
    case demo.Pong => // client's response
      for (time <- lastPingTime) {
        val elapsed = (System.currentTimeMillis() - time) / 2
        //println("Latency of player " + id + " in room " + room.id + " is " + elapsed + " ms")
        latency = Some(elapsed.toInt)
        lastPingTime = None
      }
    case x: demo.ClientPositionUpdate => updateData = Some(x)
    case x: demo.ProjectileShot       => bulletShotsData += x
    case x: demo.ProjectileHit        => bulletHitsData += x
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
          val clientMsg = upickle.read[demo.ClientMessage](text)
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
      val conn = context.actorOf(Props(classOf[ConnectionActor], curSender))
      curSender ! Http.Register(conn)
  }
}

