package games.demo.server

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
case class NetworkData(players: Seq[PlayerData])

class Updater extends Actor {
  def receive: Receive = {
    case x =>
      val players = GlobalLogic.players

      players.foreach { currentPlayer =>
        val others = players.filter { p => p != currentPlayer }
        val data = others.flatMap { p => p.data }.toSeq
        val network = NetworkData(data)
        currentPlayer.send(upickle.write[NetworkData](network))
      }

  }
}

object GlobalLogic {
  var players: Set[Player] = Set[Player]()

  def removePlayer(player: Player): Unit = this.synchronized {
    players -= player
  }

  def registerPlayer(player: Player): Int = this.synchronized {
    def genId(from: Int): Int = {
      if (!players.exists { p => p.id == from }) {
        players += player
        from
      } else genId(from + 1)
    }

    genId(1)
  }
}

class Player(val send: String => Unit) {
  val id = GlobalLogic.registerPlayer(this)
  println("Player " + id + " connected")

  var data: Option[PlayerData] = None

  def receive(msg: String): Unit = {
    data = Some(upickle.read[PlayerData](msg))
  }

  def disconnected(): Unit = {
    println("Player " + id + " disconnected")
    GlobalLogic.removePlayer(this)
  }
}

class ServiceWorker(val serverConnection: ActorRef) extends HttpServiceActor with websocket.WebSocketServerWorker {
  override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

  private var logic: Option[Player] = None

  private def sendMessage(msg: String): Unit = send(TextFrame(msg))

  def businessLogic: Receive = {
    case tf: TextFrame => logic match {
      case Some(lo) =>
        val payload = tf.payload
        val text = payload.utf8String
        if (!text.isEmpty()) lo.receive(text)

      case None => println("Warning: TextFrame received from a non-upgraded connection")
    }
    case x: FrameCommandFailed =>
      log.error("frame command failed", x)

    case UpgradedToWebSocket =>
      logic = Some(new Player(sendMessage))

    case x: Http.ConnectionClosed => logic match {
      case Some(l) => l.disconnected()
      case None    =>
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

