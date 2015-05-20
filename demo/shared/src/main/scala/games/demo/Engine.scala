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
object Absent extends PlayerState
class Playing(var position: Vector2f, var velocity: Vector2f, var orientation: Float) extends PlayerState

class Projectile(val id: Int, var position: Vector2f, val orientation: Float)

class Engine(itf: EngineInterface)(implicit ec: ExecutionContext) extends games.FrameListener {
  final val updateIntervalMs = 50 // Resend position at 20Hz
  final val shotIntervalMs = 500 // 2 shots per second
  final val configFile = "/games/demo/config"

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

  private var lastTimeUpdateFromServer: Option[Long] = None
  private var lastTimeUpdateToServer: Option[Long] = None

  private var lastTimeProjectileShot: Option[Long] = None

  private var centerVAngle: Option[Float] = None

  private var localPlayerState: PlayerState = Absent
  private var externalPlayersState: immutable.Map[Int, PlayerState] = immutable.Map()

  private var nextProjectileId = 0
  private var projectiles: mutable.Buffer[(Int, Projectile)] = mutable.Buffer()

  private def conv(v: Vector3): Vector3f = new Vector3f(v.x, v.y, v.z)
  private def conv(v: Vector3f): Vector3 = Vector3(v.x, v.y, v.z)
  private def conv(v: Vector2): Vector2f = new Vector2f(v.x, v.y)
  private def conv(v: Vector2f): Vector2 = Vector2(v.x, v.y)

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
    val configFuture = Misc.loadConfigFile(Resource(configFile))

    // Loading data
    val dataFuture = configFuture.flatMap { config =>
      this.config = config

      val modelsFuture = Rendering.loadAllModels(config("models"), gl, loopExecutionContext)
      val wallMeshFuture = Rendering.loadTriMeshFromResourceFolder("/games/demo/models/wall", gl, loopExecutionContext)
      val shadersFuture = Rendering.loadAllShaders(config("shaders"), gl, loopExecutionContext)
      val mapFuture = Map.load(Resource(config("map")))

      Future.sequence(Seq(modelsFuture, wallMeshFuture, shadersFuture, mapFuture))
    }

    // Retrieve useful data from shaders (require access to OpenGL context)
    val retrieveInfoFromDataFuture = dataFuture.map {
      case Seq(models: immutable.Map[String, OpenGLMesh], wallMesh: games.utils.SimpleOBJParser.TriMesh, shaders: immutable.Map[String, Token.Program], map: Map) =>
        itf.printLine("All data loaded successfully: " + models.size + " model(s), " + shaders.size + " shader(s)")
        itf.printLine("Map size: " + map.width + " by " + map.height)

        this.map = map
        Rendering.Standard.setup(shaders("simple3d"))
        Rendering.Wall.setup(shaders("wall"), wallMesh, map)
        Rendering.Player.setup(models("character"))
        Rendering.Bullet.setup(models("bullet"))

        Physics.setupMap(map)
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
                  val startingPosition = this.map.starts(localPlayerId).center.copy()
                  val startingOrientation = Data.initOrientation(localPlayerId)
                  this.localPlayerState = new Playing(startingPosition, new Vector2f, startingOrientation)
                  itf.printLine("You are player " + playerId)
                  helloPacketReceived.success((): Unit)
                }

              case SetPosition(spaceData) => // TODO

              case ServerUpdate(players, newEvents) =>
                lastTimeUpdateFromServer = Some(now)

                val (locals, externals) = players.partition(_.id == localPlayerId)

                assert(locals.size == 1)
                val local = locals.head

                this.externalPlayersState = externals.map { player =>
                  val id = player.id
                  val state = player.move.map { moveData =>
                    new Playing(conv(moveData.space.position), conv(moveData.velocity), moveData.space.orientation)
                  }.getOrElse(Absent)
                  (id, state)
                }.toMap

                newEvents.foreach {
                  case ProjectileCreation(ProjectileIdentifier(playerId, projId), position, orientation) =>
                    if (playerId != this.localPlayerId) this.projectiles += (playerId -> new Projectile(projId, conv(position), orientation))

                  case ProjectileDestruction(ProjectileIdentifier(playerId, projId), playerHitId) =>
                    this.projectiles = projectiles.filterNot { case (curPlayerId, curProj) => playerId == curPlayerId && projId == curProj.id }
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

    gl.enable(GLES2.CULL_FACE)
    gl.cullFace(GLES2.BACK)
    gl.frontFace(GLES2.CCW)

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
    val playing = this.localPlayerState.asInstanceOf[Playing]

    val delta = mouse.deltaPosition

    var bulletShot = false

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
          case ButtonEvent(Button.Left, true) => bulletShot = true
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

    val velocity = new Vector2f
    if (keyboard.isKeyDown(Key.W)) velocity += new Vector2f(0, 1) * -4f
    if (keyboard.isKeyDown(Key.S)) velocity += new Vector2f(0, 1) * 2f
    if (keyboard.isKeyDown(Key.D)) velocity += new Vector2f(1, 0) * 3f
    if (keyboard.isKeyDown(Key.A)) velocity += new Vector2f(1, 0) * -3f
    playing.velocity = velocity
    playing.orientation += (delta.x.toFloat / width.toFloat) * -200f

    val otherActivePlayers = externalPlayersState.flatMap {
      case (playerId, state: Playing) => Some(playerId, state)
      case _                          => None
    }
    val activePlayers = (otherActivePlayers + (this.localPlayerId -> playing))

    //#### Simulation

    // Players
    for (
      (playerId, playing) <- activePlayers
    ) {
      Physics.playerStep(playing, elapsedSinceLastFrame)
    }

    // Projectiles
    this.projectiles = this.projectiles.filter { projWithId =>
      val (shooterId, projectile) = projWithId
      val ret = Physics.projectileStep(projWithId, activePlayers, elapsedSinceLastFrame)
      if (ret >= 0 && shooterId == this.localPlayerId) {
        if (ret == 0) Console.println("You hit the wall")
        if (ret > 0) Console.println("You hit player " + ret)
      }
      if (ret > 0 && shooterId == this.localPlayerId) for (conn <- connection) {
        val hit = ProjectileHit(projectile.id, ret)
        sendMsg(hit)
      }
      ret < 0
    }

    //#### Network
    for (conn <- connection) {
      val uPosition = conv(playing.position)
      val uVelocity = conv(playing.velocity)
      val uOrientation = playing.orientation

      if (bulletShot && (lastTimeProjectileShot.isEmpty || now - lastTimeProjectileShot.get > shotIntervalMs)) {
        this.projectiles += (this.localPlayerId -> new Projectile(this.nextProjectileId, playing.position.copy(), playing.orientation))
        val shot = ProjectileShot(this.nextProjectileId, uPosition, uOrientation)
        sendMsg(shot)

        this.lastTimeProjectileShot = Some(now)
        this.nextProjectileId += 1
      }

      if (lastTimeUpdateToServer.isEmpty || now - lastTimeUpdateToServer.get > updateIntervalMs) {
        val update = ClientPositionUpdate(MoveData(SpaceData(uPosition, uOrientation), uVelocity))
        sendMsg(update)

        this.lastTimeUpdateToServer = Some(now)
      }
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

    Rendering.Standard.init()
    // Render players
    for (
      (playerId, playing) <- activePlayers if (playerId != localPlayerId)
    ) {
      val transform = Matrix4f.translate3D(new Vector3f(playing.position.x, Map.roomHalfSize, playing.position.y)) * Matrix4f.scale3D(new Vector3f(1, 1, 1) * 0.5f) * Matrix4f.rotate3D(playing.orientation, Vector3f.Up)
      Rendering.Standard.render(playerId, Rendering.Player.mesh, transform, cameraTransformInv)
    }

    // Render projectiles
    for ((playerId, projectile) <- this.projectiles) {
      val transform = Matrix4f.translate3D(new Vector3f(projectile.position.x, Map.roomHalfSize, projectile.position.y)) * Matrix4f.scale3D(new Vector3f(1, 1, 1) * 0.5f) * Matrix4f.rotate3D(projectile.orientation, Vector3f.Up)
      Rendering.Standard.render(playerId, Rendering.Bullet.mesh, transform, cameraTransformInv)
    }
    Rendering.Standard.close()

    //#### Ending
    continueCond = continueCond && itf.update()
  }
}
