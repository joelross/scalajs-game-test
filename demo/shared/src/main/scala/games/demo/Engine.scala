package games.demo

import transport.ConnectionHandle
import transport.WebSocketUrl
import games.demo.Specifics.WebSocketClient

import scala.concurrent.{ Promise, Future, ExecutionContext }
import games._
import games.math
import games.math.{ Vector3f, Vector4f, Matrix3f, Matrix4f }
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

class ShipData(var position: Vector3f, var velocity: Float, var orientation: Vector3f, var rotation: Vector3f)
class ExternalShipData(var id: Int, var data: ShipData, var latency: Int)

class BulletData(var id: Int, var shooterId: Int, var position: Vector3f, var orientation: Vector3f)

class Engine(itf: EngineInterface)(implicit ec: ExecutionContext) extends games.FrameListener {
  private val updateIntervalMs = 25 // Resend position at 40Hz
  private val shotIntervalMs = 500 // 2 shots per second max
  private val rotationMultiplier: Float = 50.0f
  private val hitDamage: Float = 25f
  private val terrainSize: Float = 50f
  private val initHealth: Float = 100f
  private val minSpeed: Float = 2f
  private val maxSpeed: Float = 6f

  def context: games.opengl.GLES2 = gl

  private var continueCond = true

  private implicit var gl: GLES2 = _
  private var audioContext: Context = _
  private var keyboard: Keyboard = _
  private var mouse: Mouse = _
  private var touchpad: Option[Touchpad] = None
  private var accelerometer: Option[Accelerometer] = None

  private var config: Map[String, String] = _

  private var connection: Option[ConnectionHandle] = None
  private var localPlayerId: Int = 0
  private var localPlayerHealth: Float = initHealth

  private var screenDim: (Int, Int) = _

  private var initPosition: Vector3f = _
  private var initOrientation: Vector3f = _

  private var localShipData: ShipData = new ShipData(new Vector3f, 0f, new Vector3f, new Vector3f)
  private var extShipsData: Map[Int, ExternalShipData] = Map()

  private var bulletsData: mutable.Map[Int, BulletData] = mutable.Map()

  private var lastTimeUpdateFromServer: Option[Long] = None
  private var lastTimeUpdateToServer: Option[Long] = None

  private var lastTimeBulletShot: Option[Long] = None

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
      val shadersFuture = Rendering.loadAllShaders("/games/demo/shaders", gl, loopExecutionContext)

      Future.sequence(Seq(modelsFuture, shadersFuture))
    }

    // Retrieve useful data from shaders (require access to OpenGL context)
    val retrieveInfoFromDataFuture = dataFuture.map {
      case Seq(models: Map[String, OpenGLMesh], shaders: Map[String, Token.Program]) =>
        itf.printLine("All data loaded successfully: " + models.size + " model(s), " + shaders.size + " shader(s)")

        Rendering.Ship.setup(shaders("simple3d"), models("ship"))
        Rendering.Bullet.setup(shaders("simple3d"), models("bullet"))
        Rendering.Health.setup(shaders("health"))
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

              case Hello(playerId, initPosition, initOrientation) =>
                if (this.connection.isEmpty) {
                  this.connection = Some(conn)
                  localPlayerId = playerId
                  localShipData.position = conv(initPosition)
                  localShipData.orientation = conv(initOrientation)
                  this.initPosition = localShipData.position.copy()
                  this.initOrientation = localShipData.orientation.copy()
                  itf.printLine("You are player " + playerId)
                  helloPacketReceived.success((): Unit)
                }

              case ServerUpdate(players, newEvents) =>
                lastTimeUpdateFromServer = Some(now)

                val (locals, externals) = players.partition(_.id == localPlayerId)

                assert(locals.size == 1)
                val local = locals.head

                extShipsData = externals.flatMap { serverUpdatePlayerData =>
                  serverUpdatePlayerData.space.map { spaceData =>
                    (serverUpdatePlayerData.id, new ExternalShipData(serverUpdatePlayerData.id, new ShipData(conv(spaceData.position), spaceData.velocity, conv(spaceData.orientation), conv(spaceData.rotation)), serverUpdatePlayerData.latency + local.latency))
                  }
                }.toMap
                newEvents.foreach {
                  case BulletCreation(shotId, shooterId, initialPosition, orientation) => bulletsData += (shotId -> new BulletData(shotId, shooterId, conv(initialPosition), conv(orientation)))
                  case BulletDestruction(shotId, playerHitId) =>
                    bulletsData.remove(shotId)
                    if (playerHitId == localPlayerId) localPlayerHealth -= hitDamage
                    if (localPlayerHealth <= 0f) { // Reset the player's ship
                      localPlayerHealth = initHealth
                      localShipData.position = this.initPosition.copy()
                      localShipData.orientation = this.initOrientation.copy()
                    }
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

    // Setup OpenGL
    gl.clearColor(0.75f, 0.75f, 0.75f, 1) // black background

    gl.enable(GLES2.DEPTH_TEST)
    gl.depthFunc(GLES2.LESS)

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

    var bulletShot = false

    //#### Update from inputs
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
              if (data.position.y < height / 4) { // If tapped in the upper part of the screen
                if (data.position.x < width / 2) {
                  gl.display.fullscreen = !gl.display.fullscreen
                } else { // Reset vertical axis center
                  centerVAngle = None // Will be set by the next accelerometer check
                }
              } else {
                bulletShot = true
              }
            case TouchEnd(data) =>
            case _              =>
          }

          processTouch() // process next event
        }
      }
      processTouch()
    }

    // Apply inputs to local ship
    if (keyboard.isKeyDown(Key.W)) localShipData.velocity = maxSpeed
    else if (keyboard.isKeyDown(Key.S)) localShipData.velocity = minSpeed
    else localShipData.velocity = (maxSpeed + minSpeed) / 2

    val mouseRotationX = (delta.x.toFloat / width.toFloat) * -rotationMultiplier
    val mouseRotationY = (delta.y.toFloat / height.toFloat) * -rotationMultiplier
    val mouseRotationXSpeed = mouseRotationX / elapsedSinceLastFrame
    val mouseRotationYSpeed = mouseRotationY / elapsedSinceLastFrame

    val accRotationSpeed = for (
      acc <- accelerometer;
      accVec <- acc.current()
    ) yield {
      val vAngle = Math.toDegrees(Math.atan2(-accVec.z, -accVec.y)).toFloat
      val hAngle = Math.toDegrees(Math.atan2(accVec.x, -accVec.y)).toFloat

      /*
       * Holding the device straight up in front of you: vAngle = 0, hAngle = 0
       * Tilting the device to the right: hAngle > 0
       * Tilting the device to the left: hAngle < 0
       * Tilting the device on his back: vAngle > 0
       * Tilting the device on his screen: vAngle < 0
       */

      if (centerVAngle.isEmpty) centerVAngle = Some(vAngle) // If there isn't any center for vAngle yet, use the current one
      val refVAngle = centerVAngle.get

      (0f, 0f)
    }

    val inputRotationXSpeed = mouseRotationXSpeed + accRotationSpeed.map(_._1).getOrElse(0f)
    val inputRotationYSpeed = mouseRotationYSpeed + accRotationSpeed.map(_._2).getOrElse(0f)

    localShipData.rotation.x = if (Math.abs(inputRotationXSpeed) > Physics.maxRotationXSpeed) Math.signum(inputRotationXSpeed) * Physics.maxRotationXSpeed else inputRotationXSpeed
    localShipData.rotation.y = if (Math.abs(inputRotationYSpeed) > Physics.maxRotationYSpeed) Math.signum(inputRotationYSpeed) * Physics.maxRotationYSpeed else inputRotationYSpeed

    //#### Simulation

    // Ships
    Physics.stepShip(elapsedSinceLastFrame, localShipData) // Local Player
    for ((shipId, shipData) <- extShipsData) { // External players
      Physics.stepShip(elapsedSinceLastFrame, shipData.data)
    }

    // Make sure the ship remains on the terrain
    if (Math.abs(localShipData.position.x) > terrainSize) localShipData.position.x = Math.signum(localShipData.position.x) * terrainSize
    if (Math.abs(localShipData.position.y) > terrainSize) localShipData.position.y = Math.signum(localShipData.position.y) * terrainSize
    if (Math.abs(localShipData.position.z) > terrainSize) localShipData.position.z = Math.signum(localShipData.position.z) * terrainSize

    // Bullets
    for ((bulletId, bulletData) <- bulletsData) {
      Physics.stepBullet(elapsedSinceLastFrame, bulletData)
    }

    // Remove bullets out of the terrain
    bulletsData --= (bulletsData.filter {
      case (bulletId, bulletData) =>
        Math.abs(bulletData.position.x) > terrainSize ||
          Math.abs(bulletData.position.y) > terrainSize ||
          Math.abs(bulletData.position.z) > terrainSize
    }).keys

    val hits: mutable.Set[(Int, Int)] = mutable.Set()
    // Check collisions for our owns bullets
    bulletsData.filter { case (bulletId, bulletData) => bulletData.shooterId == localPlayerId }.foreach {
      case (bulletId, bulletData) => extShipsData.foreach {
        case (shipId, shipData) =>
          if ((bulletData.position - shipData.data.position).length <= 1f) {
            hits += ((bulletId, shipId))
          }
      }
    }

    // Remove bullets that reached a target
    for ((bulletId, shipId) <- hits) {
      bulletsData.remove(bulletId)
    }

    //#### Network
    for (conn <- connection) {
      val position = conv(localShipData.position)
      val velocity = localShipData.velocity
      val orientation = conv(localShipData.orientation)
      val rotation = conv(localShipData.rotation)

      for (hit <- hits) {
        val (bulletId, shipId) = hit
        sendMsg(BulletHit(bulletId, shipId))
      }

      if (bulletShot && (lastTimeBulletShot.isEmpty || now - lastTimeBulletShot.get > shotIntervalMs)) {
        sendMsg(BulletShot(position, orientation))

        lastTimeBulletShot = Some(now)
      }

      if (lastTimeUpdateToServer.isEmpty || now - lastTimeUpdateToServer.get > updateIntervalMs) {
        sendMsg(ClientPositionUpdate(position, velocity, orientation, rotation))

        lastTimeUpdateToServer = Some(now)
      }
    }

    //#### Rendering
    val curDim = (width, height)
    if (curDim != screenDim) {
      screenDim = curDim
      Rendering.setProjection(width, height)
    }

    // Camera data
    val cameraOrientation = localShipData.orientation.copy()
    cameraOrientation.z = 0

    val cameraTransform = Matrix4f.translate3D(localShipData.position) * Physics.matrixForOrientation(cameraOrientation).toHomogeneous()
    val cameraTransformInv = cameraTransform.invertedCopy()

    // Clear the buffers
    val r = Physics.interpol(localPlayerHealth, 100f, 0f, 0.75f, 1.0f)
    val gb = Physics.interpol(localPlayerHealth, 100f, 0f, 0.75f, 0.0f)
    gl.clearColor(r, gb, gb, 1f) // Background color depending of player's current health

    gl.clear(GLES2.COLOR_BUFFER_BIT | GLES2.DEPTH_BUFFER_BIT)

    // Ships
    Rendering.Ship.init()
    for ((extId, shipData) <- extShipsData) {
      val transform = Matrix4f.translate3D(shipData.data.position) * Physics.matrixForOrientation(shipData.data.orientation).toHomogeneous()
      Rendering.Ship.render(extId, transform, cameraTransformInv)
    }
    Rendering.Ship.close()

    // Bullets
    Rendering.Bullet.init()
    for ((bulletId, bulletData) <- bulletsData) {
      val transform = Matrix4f.translate3D(bulletData.position) * Physics.matrixForOrientation(bulletData.orientation).toHomogeneous()
      Rendering.Bullet.render(bulletData.shooterId, transform, cameraTransformInv)
    }
    Rendering.Bullet.close()

    //Hud: health
    Rendering.Health.init()
    Rendering.Health.render(localPlayerHealth)
    Rendering.Health.close()

    //#### Ending
    continueCond = continueCond && itf.update()
  }
}
