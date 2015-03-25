package games.demo.server

import akka.actor.ActorRef
import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._
import spray.can.websocket
import spray.can.websocket.frame.BinaryFrame
import spray.can.websocket.FrameCommandFailed
import spray.can.websocket.frame.TextFrame
import akka.actor.ActorRefFactory
import spray.can.Http
import akka.actor.Props

class Service extends Actor {
  def receive = {
    case Http.Connected(remoteAddress, localAddress) => {
      val curSender = sender()
      val conn = context.actorOf(Props(classOf[ServiceWorker], curSender))
      curSender ! Http.Register(conn)
    }
  }
}

class ServiceWorker(val serverConnection: ActorRef) extends HttpServiceActor with websocket.WebSocketServerWorker {

  override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

  def businessLogic: Receive = {
    case bf: BinaryFrame => {
      val curSender = sender()
      println("BinaryFrame received from " + curSender)
    }
    case tf: TextFrame => {
      val curSender = sender()
      val payload = tf.payload
      val text = payload.utf8String
      println("TextFrame received from " + curSender + ": " + text)
      val message = TextFrame("Hello from server")
      curSender ! message
    }
    case x: FrameCommandFailed =>
      log.error("frame command failed", x)
    case x: HttpRequest => // do something
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
