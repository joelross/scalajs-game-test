package games.demo

import transport.ConnectionHandle
import transport.WebSocketUrl
import games.demo.Specifics.WebSocketClient

import scala.concurrent.{ Future, ExecutionContext }
import games._
import games.math
import games.math.{ Vector3f, Vector4f, Matrix4f, Matrix3f }
import games.opengl._
import games.audio._
import games.input._
import games.utils._
import java.nio.{ ByteBuffer, FloatBuffer, ByteOrder }
import games.opengl.GLES2Debug
import games.audio.Source3D
import games.input.ButtonEvent
import games.audio.AbstractSource
import scala.collection.mutable.ArrayBuffer

abstract class EngineInterface {
  def printLine(msg: String): Unit
  def initGL(): GLES2
  def initAudio(): Context
  def initKeyboard(): Keyboard
  def initMouse(): Mouse
  def update(): Boolean
  def close(): Unit
}

class Engine(itf: EngineInterface)(implicit ec: ExecutionContext) extends games.FrameListener {
  def context: games.opengl.GLES2 = gl

  private var continueCond = true

  private var gl: GLES2 = _
  private var audioContext: Context = _
  private var keyboard: Keyboard = _
  private var mouse: Mouse = _

  private var connection: Option[ConnectionHandle] = None
  private var localPlayerId: Option[Int] = None

  def continue(): Boolean = continueCond

  def onClose(): Unit = {
    itf.printLine("Closing...")
    itf.close()

    mouse.close()
    keyboard.close()
    audioContext.close()
    gl.close()

    for (conn <- connection) {
      conn.close()
      connection = None
    }
  }

  def onCreate(): Option[Future[Unit]] = {
    itf.printLine("Starting...")
    this.gl = new GLES2Debug(itf.initGL()) // Init OpenGL (Enable automatic error checking by encapsuling it in GLES2Debug)
    this.audioContext = itf.initAudio() // Init Audio
    this.keyboard = itf.initKeyboard() // Init Keyboard listening
    this.mouse = itf.initMouse() // Init Mouse listener

    audioContext.volume = 0.25f // Lower the initial global volume

    // Init network
    val futureConnection = new WebSocketClient().connect(WebSocketUrl(Data.server))
    futureConnection.onSuccess {
      case conn =>
        itf.printLine("Websocket connection established")
        this.connection = Some(conn)
        conn.handlerPromise.success { msg =>
          /*val serverMsg = upickle.read[ServerMessage](msg)

          serverMsg match {
            case Hello(playerId) =>
              localPlayerId = Some(playerId)
              itf.printLine("You are player " + playerId)

            case ServerUpdate(players, newEvents) =>
            // TODO process update
          }*/
        }
        conn.closedFuture.onSuccess {
          case _ =>
            itf.printLine("Websocket connection closed")
            this.connection = None
        }
    }

    // TODO
    None
  }

  def onDraw(fe: games.FrameEvent): Unit = {
    if (keyboard.isKeyDown(Key.Escape)) {
      continueCond = false
    }

    // TODO

    continueCond = continueCond && itf.update()
  }
}
