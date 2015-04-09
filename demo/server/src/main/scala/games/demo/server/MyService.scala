package games.demo.server

import games.demo

import akka.actor.ActorRef
import akka.actor.Actor
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

import scala.collection.immutable.Set

import scala.concurrent.duration._

case class PlayerData(posX: Float, posY: Float, posZ: Float, rotH: Float, rotV: Float)

sealed trait LocalMessage
case class Disconnected() extends LocalMessage

class Updater extends Actor {
  def receive: Receive = {
    case x =>
      val players = GlobalLogic.players

      players.foreach { currentPlayer =>
        val others = players.filter { p => p != currentPlayer }
        val data = others.flatMap { p => p.data }.toSeq
        //        val network = NetworkData(data)
        //        currentPlayer.send(upickle.write[NetworkData](network))
      }

  }
}

object GlobalLogic {
  var players: Set[Player] = Set[Player]()
  var currentRoom: Room = new Room(0)

  def newRoom(): ActorRef = {
    ???
  }

  def removePlayer(player: Player): Unit = this.synchronized {
    players -= player
  }

  def registerPlayer(player: Player): Unit = this.synchronized {
    val newPlayerId = currentRoom.registerPlayer(player)
    val newPlayerRoom = currentRoom

    if (currentRoom.players.size >= 2) { // Current room is full, create a new one
      currentRoom = new Room(currentRoom.id + 1)
    }

    player.id = newPlayerId
    player.room = newPlayerRoom
  }
}

class Room(val id: Int) {
  var players: Set[Player] = Set[Player]()

  def registerPlayer(player: Player): Int = {
    def genId(from: Int): Int = {
      if (!players.exists { p => p.id == from }) {
        players += player
        from
      } else genId(from + 1)
    }

    genId(1)
  }
}

class Player(sendFun: String => Unit) extends Actor {
  var id: Int = 0
  var room: Room = _

  GlobalLogic.registerPlayer(this)

  println("Player " + id + " connected to room " + room.id)
  send(demo.Hello(id, demo.Vector3(0, 0, 0), demo.Vector3(0, 0, 0)))

  var data: Option[demo.ClientUpdate] = None

  def send(msg: demo.ServerMessage): Unit = {
    val data = upickle.write(msg)
    sendFun(data)
  }

  def receive: Receive = {
    // Client Message
    case demo.Pong()          => // client's response
    case demo.KeepAlive()     => // nothing to do
    case x: demo.ClientUpdate => data = Some(x) // update local data of the player
    case x: demo.BulletShot   => // handle data

    // Local message
    case Disconnected =>
      println("Player " + id + " disconnected from room " + room.id)
      GlobalLogic.removePlayer(this)
  }
}

class ServiceWorker(val serverConnection: ActorRef) extends HttpServiceActor with websocket.WebSocketServerWorker {
  override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

  private var playerActor: Option[ActorRef] = None

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
      val actor = context.actorOf(Props(classOf[Player], sendMessage _))
      playerActor = Some(actor)

    case x: Http.ConnectionClosed => playerActor match {
      case Some(actor) => actor ! Disconnected
      case None        =>
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

