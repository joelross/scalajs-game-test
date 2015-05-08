package games.demo

import transport.ConnectionHandle
import transport.WebSocketUrl
import games.demo.Specifics.WebSocketClient

import scala.concurrent.{ Promise, Future, ExecutionContext }
import games._
import games.math._
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

import scala.collection.immutable
import scala.collection.mutable

abstract class EngineInterface {
  def printLine(msg: String): Unit
  def initGL(): GLES2
  def initAudio(): Context
  def initKeyboard(): Keyboard
  def initMouse(): Mouse
  def initTouch(): Option[Touchpad]
  def initAccelerometer(): Option[Accelerometer]
  def update(): Boolean
  def close(): Unit
}

sealed abstract class PlayerState
object Away extends PlayerState
class Playing(var position: Vector2f, var orientation: Float) extends PlayerState

class Engine(itf: EngineInterface)(implicit ec: ExecutionContext) extends games.FrameListener {
  val updateIntervalMs = 50 // Resend position at 20Hz

  def context: games.opengl.GLES2 = gl

  private var continueCond = true

  private implicit var gl: GLES2 = _
  private var audioContext: Context = _
  private var keyboard: Keyboard = _
  private var mouse: Mouse = _
  private var touchpad: Option[Touchpad] = None
  private var accelerometer: Option[Accelerometer] = None

  private var config: immutable.Map[String, String] = _

  private var connection: Option[ConnectionHandle] = None

  private var screenDim: (Int, Int) = _

  private var map: Map = _

  private var localPlayerId: Int = 0
  private var currentState: PlayerState = Away

  private var lastTimeUpdateFromServer: Option[Long] = None
  private var lastTimeUpdateToServer: Option[Long] = None

  private var lastTimeProjectileShot: Option[Long] = None

  private var centerVAngle: Option[Float] = None

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

    for (acc <- accelerometer) acc.close()
    for (touch <- touchpad) touch.close()
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
    this.gl = itf.initGL() // Init OpenGL (Enable automatic error checking by encapsuling it in GLES2Debug)
    this.audioContext = itf.initAudio() // Init Audio
    this.keyboard = itf.initKeyboard() // Init Keyboard listening
    this.mouse = itf.initMouse() // Init Mouse listener
    this.touchpad = itf.initTouch() // Init touch (if available)
    this.accelerometer = itf.initAccelerometer() // Init accelerometer (if available)

    audioContext.volume = 0.25f // Lower the initial global volume

    // Load main config file
    val configFuture = Misc.loadConfigFile(Resource("/games/demo/config"))

    // Loading data
    val dataFuture = configFuture.flatMap { config =>
      this.config = config

      val modelsFuture = Rendering.loadAllModels("/games/demo/models", gl, loopExecutionContext)
      val wallMeshFuture = Rendering.loadTriMeshFromResourceFolder("/games/demo/models/wall", gl, loopExecutionContext)
      val shadersFuture = Rendering.loadAllShaders("/games/demo/shaders", gl, loopExecutionContext)
      val mapFuture = Map.load(Resource("/games/demo/maps/map1"))

      Future.sequence(Seq(modelsFuture, wallMeshFuture, shadersFuture, mapFuture))
    }

    // Retrieve useful data from shaders (require access to OpenGL context)
    val retrieveInfoFromDataFuture = dataFuture.map {
      case Seq(models: immutable.Map[String, OpenGLMesh], wallMesh: games.utils.SimpleOBJParser.TriMesh, shaders: immutable.Map[String, Token.Program], map: Map) =>
        itf.printLine("All data loaded successfully: " + models.size + " model(s), " + shaders.size + " shader(s)")
        itf.printLine("Map size: " + map.width + " by " + map.height)

        this.map = map
        Rendering.Wall.setup(shaders("wall"), wallMesh, map)
        Rendering.Standard.setup(shaders("simple3d"))
    }(loopExecutionContext)

    val helloPacketReceived = Promise[Unit]

    // Init network (wait for data loading to complete before that)
    val networkFuture = retrieveInfoFromDataFuture.flatMap { _ =>
      val serverAddress = config("server")
      itf.printLine("Server address: " + serverAddress)

      val futureConnection = new WebSocketClient().connect(WebSocketUrl(serverAddress))
      futureConnection.map { conn =>
        itf.printLine("Websocket connection established")
        // Wait for the Hello packet to register the connection
        conn.handlerPromise.success { msg =>
          val now = System.currentTimeMillis()
          val serverMsg = upickle.read[ServerMessage](msg)

          Future { // To avoid concurrency issue, process the following in the loop thread
            serverMsg match {
              case Ping => // answer that ASAP
                sendMsg(Pong)

              case Hello(playerId) =>
                if (this.connection.isEmpty) {
                  this.connection = Some(conn)
                  this.localPlayerId = playerId
                  this.currentState = new Playing(this.map.starts(localPlayerId).center.copy(), 0f)
                  itf.printLine("You are player " + playerId)
                  helloPacketReceived.success((): Unit)
                }

              case ServerUpdate(players, newEvents) =>
                lastTimeUpdateFromServer = Some(now)

                val (locals, externals) = players.partition(_.id == localPlayerId)

                assert(locals.size == 1)
                val local = locals.head

                // TODO process players externals

                newEvents.foreach {
                  case ProjectileCreation(projId, position, orientation) =>
                  // TODO
                  case ProjectileDestruction(projId) =>
                  // TODO
                  case PlayerHit(playerId) =>
                  // TODO
                  case _ =>
                }
            }
          }(loopExecutionContext)

        }
        conn.closedFuture.onSuccess {
          case _ =>
            itf.printLine("Websocket connection closed")
            this.connection = None
        }
      }
    }.flatMap { _ => helloPacketReceived.future }

    gl.enable(GLES2.DEPTH_TEST)
    gl.depthFunc(GLES2.LESS)

    gl.clearColor(0.75f, 0.75f, 0.75f, 1f) // Grey background

    val width = gl.display.width
    val height = gl.display.height

    screenDim = (width, height)
    Rendering.setProjection(width, height)

    Some(networkFuture) // wait for network setup (last part) to complete before proceding
  }

  def onDraw(fe: games.FrameEvent): Unit = {
    val now = System.currentTimeMillis()
    val elapsedSinceLastFrame = fe.elapsedTime

    val width = gl.display.width
    val height = gl.display.height

    //#### Update from inputs
    val playing = this.currentState.asInstanceOf[Playing]

    val delta = mouse.deltaPosition

    def processKeyboard() {
      val optKeyEvent = keyboard.nextEvent()
      for (keyEvent <- optKeyEvent) {
        if (keyEvent.down) keyEvent.key match {
          case Key.Escape => continueCond = false
          case Key.L      => mouse.locked = !mouse.locked
          case Key.F      => gl.display.fullscreen = !gl.display.fullscreen
          case _          => // nothing to do
        }

        processKeyboard() // process next event
      }
    }
    processKeyboard()

    def processMouse() {
      val optMouseEvent = mouse.nextEvent()
      for (mouseEvent <- optMouseEvent) {
        mouseEvent match {
          case ButtonEvent(Button.Left, true) =>
          case _                              =>
        }

        processMouse() // process next event
      }
    }
    processMouse()

    for (touchpad <- this.touchpad) {
      def processTouch() {
        val optTouchEvent = touchpad.nextEvent()
        for (touchEvent <- optTouchEvent) {
          touchEvent match {
            case TouchStart(data) =>
            case TouchEnd(data)   =>
            case _                =>
          }

          processTouch() // process next event
        }
      }
      processTouch()
    }

    playing.orientation += delta.x.toFloat / width.toFloat * -100f
    val playerRotation = Matrix2f.rotate2D(-playing.orientation)
    val movement = new Vector2f
    if (keyboard.isKeyDown(Key.W)) movement += new Vector2f(0, 1) * (-4f * elapsedSinceLastFrame)
    if (keyboard.isKeyDown(Key.S)) movement += new Vector2f(0, 1) * (2f * elapsedSinceLastFrame)
    if (keyboard.isKeyDown(Key.D)) movement += new Vector2f(1, 0) * (3f * elapsedSinceLastFrame)
    if (keyboard.isKeyDown(Key.A)) movement += new Vector2f(1, 0) * (-3f * elapsedSinceLastFrame)
    playing.position += (playerRotation * movement)

    //#### Simulation

    //#### Network
    for (conn <- connection) {
      // TODO
    }

    //#### Rendering
    val curDim = (width, height)
    if (curDim != screenDim) {
      screenDim = curDim
      Rendering.setProjection(width, height)
    }

    // Clear the buffers
    gl.clear(GLES2.COLOR_BUFFER_BIT | GLES2.DEPTH_BUFFER_BIT)

    // Camera data
    val cameraTransform = Matrix4f.translate3D(new Vector3f(playing.position.x, Map.roomHalfSize, playing.position.y)) * Matrix4f.rotate3D(playing.orientation, Vector3f.Up)
    val cameraTransformInv = cameraTransform.invertedCopy()

    Rendering.Wall.init()
    Rendering.Wall.render(cameraTransformInv)
    Rendering.Wall.close()

    //#### Ending
    continueCond = continueCond && itf.update()
  }
}
