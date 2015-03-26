package games.demo.server

import akka.actor.ActorRef
import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._
import spray.can.websocket
import spray.can.websocket.frame.BinaryFrame
import spray.can.websocket.FrameCommandFailed
import spray.can.websocket.UpgradedToWebSocket
import spray.can.websocket.frame.TextFrame
import spray.can.websocket.FrameCommand
import akka.actor.ActorRefFactory
import spray.can.Http
import akka.actor.Props

class Service extends Actor {

  def receive = {
    case Http.Connected(remoteAddress, localAddress) => {
      val curSender = sender()
      println("Connection from " + curSender)
      val conn = context.actorOf(Props(classOf[ServiceWorker], curSender))
      curSender ! Http.Register(conn)
    }
  }
}

class PlayerLogic() {
  println("Player connected")

  def incomingFrame(msg: String): Unit = {
    println("TextFrame received: " + msg)
  }

  def disconnected(): Unit = {
    println("Player disconnected")
  }
}

class ServiceWorker(val serverConnection: ActorRef) extends HttpServiceActor with websocket.WebSocketServerWorker {
  override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

  private var logic: Option[PlayerLogic] = None

  def businessLogic: Receive = {
    case bf: BinaryFrame => {
      println("BinaryFrame received from " + sender())
    }
    case tf: TextFrame => logic match {
      case Some(lo) => {
        val payload = tf.payload
        val text = payload.utf8String
        lo.incomingFrame(text)
        println("Sender: " + sender())
        sender ! tf
      }
      case None => println("Warning: TextFrame received from a non-upgraded connection")
    }
    case x: FrameCommandFailed =>
      log.error("frame command failed", x)
    case UpgradedToWebSocket => {
      logic = Some(new PlayerLogic)
      val cur = sender()
      println("Websocket setup: " + cur)
    }
    case Http.Closed => logic match {
      case Some(l) => l.disconnected()
      case None    =>
    }
    case x => println("Other: " + x + " from " + sender)
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
