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

import scala.collection.mutable.Set

class Service extends Actor {

  def receive = {
    case Http.Connected(remoteAddress, localAddress) => {
      val curSender = sender()
      val conn = context.actorOf(Props(classOf[ServiceWorker], curSender))
      curSender ! Http.Register(conn)
    }
  }
}

object GlobalPlayerLogic {
  private val ids: Set[Int] = Set[Int]()

  def removeId(id: Int): Unit = this.synchronized {
    ids -= id
  }

  def getNewId(): Int = this.synchronized {
    def genId(from: Int): Int = {
      if (!ids(from)) {
        ids += from
        from
      } else genId(from + 1)
    }

    genId(1)
  }
}

class PlayerLogic(val send: String => Unit) {
  val id = GlobalPlayerLogic.getNewId()

  println("Player " + id + " connected")
  send("Hello, you are player " + id)

  def receive(msg: String): Unit = {
    send("Echoing back: " + msg)
  }

  def disconnected(): Unit = {
    println("Player " + id + " disconnected")
    GlobalPlayerLogic.removeId(id)
  }
}

class ServiceWorker(val serverConnection: ActorRef) extends HttpServiceActor with websocket.WebSocketServerWorker {
  override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

  private var logic: Option[PlayerLogic] = None

  private def sendMessage(msg: String): Unit = send(TextFrame(msg))

  def businessLogic: Receive = {
    case tf: TextFrame => logic match {
      case Some(lo) => {
        val payload = tf.payload
        val text = payload.utf8String
        lo.receive(text)
      }
      case None => println("Warning: TextFrame received from a non-upgraded connection")
    }
    case x: FrameCommandFailed => {
      log.error("frame command failed", x)
    }
    case UpgradedToWebSocket => {
      logic = Some(new PlayerLogic(sendMessage))
    }
    case Http.Closed => logic match {
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
