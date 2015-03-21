package games.demo.server

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._

class MyServiceActor extends Actor with MyService {

  def actorRefFactory = context

  def receive = runRoute(myRoute)
}

trait MyService extends HttpService {

  val myRoute =
    path("") {
      respondWithMediaType(`text/html`) {
        getFromFile("../demoJS-launcher/index.html")
      }
    } ~
      path("fast") {
        respondWithMediaType(`text/html`) {
          getFromFile("../demoJS-launcher/index-fastopt.html")
        }
      } ~
      path("code" / Rest) { file =>
        val path = "../demo/js/target/scala-2.11/" + file
        getFromFile(path)
      } ~
      path("resources" / Rest) { file =>
        val path = "../demo/shared/src/main/resources/" + file
        getFromFile(path)
      }
}
