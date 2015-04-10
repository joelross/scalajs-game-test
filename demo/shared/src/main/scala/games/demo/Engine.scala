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
import scala.collection.mutable
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

class Engine(itf: EngineInterface, localEC: ExecutionContext, parEC: ExecutionContext) extends games.FrameListener {
  private implicit val standardEC = parEC
  private val updateIntervalMs = 25 // Resend position at 40Hz

  def context: games.opengl.GLES2 = gl

  private var continueCond = true

  private var gl: GLES2 = _
  private var audioContext: Context = _
  private var keyboard: Keyboard = _
  private var mouse: Mouse = _

  private var connection: Option[ConnectionHandle] = None
  private var localPlayerId: Int = 0

  private var currentPosition: Vector3f = new Vector3f(0, 0, 0)
  private var currentOrientationX: Float = 0
  private var currentOrientationY: Float = 0
  private var currentOrientationZ: Float = 0

  private var otherPlayers: Seq[PlayerServerUpdate] = Seq()
  private var lastTimeUpdateFromServer: Option[Long] = None
  private var lastTimeUpdateToServer: Option[Long] = None

  private def conv(v: Vector3): Vector3f = new Vector3f(v.x, v.y, v.z)
  private def conv(v: Vector3f): Vector3 = Vector3(v.x, v.y, v.z)

  def sendMsg(msg: ClientMessage): Unit = connection match {
    case None => throw new RuntimeException("Websocket not connected")
    case Some(conn) =>
      val data = upickle.write(msg)
      conn.write(data)
  }

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

  def onCreate(): Option[Future[Unit]] = try {
    itf.printLine("Starting...")
    this.gl = new GLES2Debug(itf.initGL()) // Init OpenGL (Enable automatic error checking by encapsuling it in GLES2Debug)
    this.audioContext = itf.initAudio() // Init Audio
    this.keyboard = itf.initKeyboard() // Init Keyboard listening
    this.mouse = itf.initMouse() // Init Mouse listener

    audioContext.volume = 0.25f // Lower the initial global volume

    // Loading data
    val sphereFuture = DemoUtils.loadModelFromResourceFolder("/games/demo/sphere", gl, localEC)
    sphereFuture.onSuccess { case mesh => Console.println("Sphere loaded successfully") }
    sphereFuture.onFailure { case t => t.printStackTrace() }

    // Init network
    val futureConnection = new WebSocketClient().connect(WebSocketUrl(Data.server))
    futureConnection.onSuccess {
      case conn =>
        itf.printLine("Websocket connection established")
        this.connection = Some(conn)
        conn.handlerPromise.success { msg =>
          val serverMsg = upickle.read[ServerMessage](msg)

          serverMsg match {
            case Ping => // answer that ASAP
              sendMsg(Pong)

            case Hello(playerId, initPos, initDir) =>
              localPlayerId = playerId
              currentPosition = conv(initPos)
              currentOrientationX = initDir.x
              currentOrientationY = initDir.y
              currentOrientationZ = initDir.z
              itf.printLine("You are player " + playerId)

            case ServerUpdate(players, newEvents) =>
              lastTimeUpdateFromServer = Some(System.currentTimeMillis())
              otherPlayers = players.filter { _.id != localPlayerId }
              newEvents.foreach { event =>
                // TODO process event
              }
          }
        }
        conn.closedFuture.onSuccess {
          case _ =>
            itf.printLine("Websocket connection closed")
            this.connection = None
        }
    }

    // TODO

    None
  } catch {
    case t: Throwable => Some(Future.failed(throw new RuntimeException("Could not init game engine", t)))
  }

  def onDraw(fe: games.FrameEvent): Unit = {
    val now = System.currentTimeMillis()
    val elapsedSinceLastFrame = fe.elapsedTime

    val width = gl.display.width
    val height = gl.display.height

    // Update from inputs
    val delta = mouse.deltaPosition

    def processKeyboard() {
      val optKeyEvent = keyboard.nextEvent()
      for (keyEvent <- optKeyEvent) {
        if (keyEvent.down) keyEvent.key match {
          case Key.Escape => continueCond = false
        }

        processKeyboard() // process next event
      }
    }
    processKeyboard()

    // Simulation

    // Network (if necessary)
    for (conn <- connection) {
      if (lastTimeUpdateToServer.isEmpty || now - lastTimeUpdateToServer.get > updateIntervalMs) {
        val position = Vector3(currentPosition.x, currentPosition.y, currentPosition.z)
        val velocity = 0f
        val orientation = Vector3(currentOrientationX, currentOrientationY, currentOrientationZ)
        val rotation = Vector3(0, 0, 0)
        val clientUpdate = ClientUpdate(position, velocity, orientation, rotation)

        val msgText = upickle.write(clientUpdate)
        conn.write(msgText)

        lastTimeUpdateToServer = Some(now)
      }
    }

    // Rendering

    continueCond = continueCond && itf.update()
  }
}
