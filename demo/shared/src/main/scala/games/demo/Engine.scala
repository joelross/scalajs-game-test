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
  def initGL(): GLES2
  def initAudio(): Context
  def initKeyboard(): Keyboard
  def initMouse(): Mouse
  def initTouch(): Option[Touchscreen]
  def initAccelerometer(): Option[Accelerometer]
  def continue(): Boolean
}

sealed abstract class State
object Absent extends State
class Present(var position: Vector2f, var velocity: Vector2f, var orientation: Float, var health: Float) extends State

class Projectile(val id: Int, var position: Vector2f, val orientation: Float)

class Engine(itf: EngineInterface)(implicit ec: ExecutionContext) extends games.FrameListener {
  final val updateIntervalMs: Int = 50 // Resend position at 20Hz
  final val shotIntervalMs: Int = 500 // 2 shots per second
  final val invulnerabilityTimeMs: Int = 10000 // 10 seconds of invulnerability when spawning
  final val configFile: String = "/games/demo/config"
  final val initialHealth: Float = 100f
  final val damagePerShot: Float = 20f // 5 shots to destroy

  final val maxForwardSpeed: Float = 4f
  final val maxBackwardSpeed: Float = 2f
  final val maxLateralSpeed: Float = 3f

  final val maxTouchTimeToShootMS: Int = 100

  def context: games.opengl.GLES2 = gl

  private var continueCond = true

  private implicit var gl: GLES2 = _
  private var audioContext: Context = _
  private var keyboard: Keyboard = _
  private var mouse: Mouse = _
  private var touchscreen: Option[Touchscreen] = None
  private var accelerometer: Option[Accelerometer] = None

  private var config: immutable.Map[String, String] = _

  private var connection: Option[ConnectionHandle] = None

  private var audioSimpleSource: audio.Source = _
  private var audioSources3D: mutable.Map[Int, audio.Source3D] = mutable.Map()
  private var audioPlayers: mutable.Set[audio.Player] = mutable.Set()
  private var audioShootData: audio.BufferedData = _
  private var audioDamageData: audio.BufferedData = _

  private var screenDim: (Int, Int) = _

  private var map: Map = _

  private var localPlayerId: Int = 0

  private var lastTimeUpdateFromServer: Option[Long] = None
  private var lastTimeUpdateToServer: Option[Long] = None

  private var lastTimeProjectileShot: Option[Long] = None
  private var lastTimeSpawn: Option[Long] = None
  private var lastTimeHit: Option[Long] = None

  private var centerVAngle: Option[Float] = None

  private var layout: KeyLayout = Qwerty

  private var localPlayerState: State = Absent
  private var externalPlayersState: immutable.Map[Int, State] = immutable.Map()

  private var nextProjectileId = 0
  private var projectiles: mutable.Buffer[(Int, Projectile)] = mutable.Buffer()

  private var moveTouch: Option[(Int, input.Position)] = None
  private var orientationTouch: Option[(Int, input.Position)] = None
  private val timeTouches: mutable.Map[Int, Long] = mutable.Map()

  private def ifPresent[T](action: Present => T): Option[T] = this.localPlayerState match {
    case x: Present => Some(action(x))
    case _          => None // nothing to do
  }

  private def sendMsg(msg: network.ClientMessage): Unit = connection match {
    case None => throw new RuntimeException("Websocket not connected")
    case Some(conn) =>
      val data = upickle.write(msg)
      conn.write(data)
  }

  private def getOrCreateSource3D(playerId: Int): audio.Source3D = this.audioSources3D.get(playerId) match {
    case None =>
      val source3d = this.audioContext.createSource3D()
      this.audioSources3D += (playerId -> source3d)
      source3d

    case Some(source3d) => source3d
  }

  def continue(): Boolean = continueCond

  def onClose(): Unit = {
    Console.println("Closing...")

    for (acc <- accelerometer) acc.close()
    for (touch <- touchscreen) touch.close()
    mouse.close()
    keyboard.close()
    audioContext.close()
    gl.close()

    for (conn <- connection) {
      conn.close()
      connection = None
    }
  }

  def onCreate(): Future[Unit] = {
    Console.println("Starting...")
    this.gl = itf.initGL() // Init OpenGL (Enable automatic error checking by encapsuling it in GLES2Debug)
    this.audioContext = itf.initAudio() // Init Audio
    this.keyboard = itf.initKeyboard() // Init Keyboard listening
    this.mouse = itf.initMouse() // Init Mouse listener
    this.touchscreen = itf.initTouch() // Init touch (if available)
    this.accelerometer = itf.initAccelerometer() // Init accelerometer (if available)

    audioContext.volume = 0.5f // Lower the initial global volume

    // Load main config file
    val configFuture = Misc.loadConfigFile(Resource(configFile))

    // Loading data
    val dataFuture = configFuture.flatMap { config =>
      this.config = config

      val modelsFuture = Rendering.loadAllModels(config("models"), gl, loopExecutionContext)
      val wallMeshFuture = Rendering.loadTriMeshFromResourceFolder("/games/demo/models/wall", gl, loopExecutionContext)
      val floorMeshFuture = Rendering.loadTriMeshFromResourceFolder("/games/demo/models/floor", gl, loopExecutionContext)
      val shadersFuture = Rendering.loadAllShaders(config("shaders"), gl, loopExecutionContext)
      val mapFuture = Map.load(Resource(config("map")))
      val audioShootDataFuture = audioContext.prepareBufferedData(Resource(config("shootSound")))
      val audioDamageDataFuture = audioContext.prepareBufferedData(Resource(config("damageSound")))

      Future.sequence(Seq(modelsFuture, wallMeshFuture, floorMeshFuture, shadersFuture, mapFuture, audioShootDataFuture, audioDamageDataFuture))
    }

    // Retrieve useful data from shaders (require access to OpenGL context)
    val retrieveInfoFromDataFuture = dataFuture.map {
      case Seq(models: immutable.Map[String, OpenGLMesh], wallMesh: games.utils.SimpleOBJParser.TriMesh, floorMesh: games.utils.SimpleOBJParser.TriMesh, shaders: immutable.Map[String, Token.Program], map: Map, audioShootData: audio.BufferedData, audioDamageData: audio.BufferedData) =>
        Console.println("All data loaded successfully: " + models.size + " model(s), " + shaders.size + " shader(s), audio ready")
        Console.println("Map size: " + map.width + " by " + map.height)

        this.map = map

        // Graphics
        Rendering.Hud.setup(shaders("simple2d"))
        Rendering.Standard.setup(shaders("simple3d"))
        Rendering.Map.setup(shaders("simple3d"), wallMesh, floorMesh, map)
        Rendering.Player.setup(models("character"))
        Rendering.Bullet.setup(models("bullet"))

        // Physics
        Physics.setupMap(map)

        // Audio
        this.audioShootData = audioShootData
        this.audioDamageData = audioDamageData
        this.audioSimpleSource = audioContext.createSource()
    }(loopExecutionContext)

    val helloPacketReceived = Promise[Unit]

    // Init network (wait for data loading to complete before that)
    val networkFuture = retrieveInfoFromDataFuture.flatMap { _ =>
      val serverAddress = config("server")
      Console.println("Connecting to " + serverAddress)

      val futureConnection = new WebSocketClient().connect(WebSocketUrl(serverAddress))
      futureConnection.map { conn =>
        Console.println("Websocket connection established")
        // Wait for the Hello packet to register the connection
        conn.handlerPromise.success { msg =>
          val now = System.currentTimeMillis()
          val serverMsg = upickle.read[network.ServerMessage](msg)

          Future { // To avoid concurrency issue, process the following in the loop thread
            serverMsg match {
              case network.ServerPing => // answer that ASAP
                sendMsg(network.ClientPong)

              case network.ServerHello(playerId) =>
                if (this.connection.isEmpty) {
                  this.connection = Some(conn)
                  this.localPlayerId = playerId
                  val startingPosition = this.map.startPositions(localPlayerId).center.copy()
                  val startingOrientation = this.map.startOrientations(localPlayerId)
                  this.localPlayerState = new Present(startingPosition, new Vector2f, startingOrientation, initialHealth)
                  this.lastTimeSpawn = Some(now)
                  Console.println("You are player " + playerId)
                  helloPacketReceived.success((): Unit)
                }

              case network.ServerUpdate(players, newEvents) =>
                lastTimeUpdateFromServer = Some(now)

                val (locals, externals) = players.partition(_.id == localPlayerId)

                assert(locals.size == 1)
                val local = locals.head

                this.externalPlayersState = externals.map { player =>
                  val id = player.id
                  val state = Misc.conv(player.state)
                  (id, state)
                }.toMap

                newEvents.foreach {
                  case network.ProjectileShot(network.ProjectileIdentifier(playerId, projId), position, orientation) =>
                    if (playerId != this.localPlayerId) {
                      this.projectiles += (playerId -> new Projectile(projId, Misc.conv(position), orientation))
                      val source3d = getOrCreateSource3D(playerId)

                      val player = this.audioShootData.attachNow(source3d)
                      player.playing = true
                      this.audioPlayers += player
                    }

                  case network.ProjectileHit(network.ProjectileIdentifier(playerId, projId), playerHitId) =>
                    if (playerHitId == this.localPlayerId) {

                      if (this.lastTimeSpawn.isDefined && (now - this.lastTimeSpawn.get) > invulnerabilityTimeMs) {
                        ifPresent { present =>
                          present.health -= damagePerShot
                          if (present.health <= 0f) { // Reset the player
                            val startingPosition = this.map.startPositions(localPlayerId).center.copy()
                            val startingOrientation = this.map.startOrientations(localPlayerId)

                            present.health = this.initialHealth
                            present.position = startingPosition
                            present.orientation = startingOrientation
                            this.lastTimeSpawn = Some(now)
                          }

                          val player = this.audioDamageData.attachNow(this.audioSimpleSource)
                          player.playing = true
                          this.audioPlayers += player
                        }
                      }
                    }
                    this.projectiles = projectiles.filterNot { case (curPlayerId, curProj) => playerId == curPlayerId && projId == curProj.id }
                }
            }
          }(loopExecutionContext)

        }
        conn.closedFuture.onSuccess {
          case _ =>
            Console.println("Websocket connection closed")
            this.connection = None
        }
      }
    }.flatMap { _ => helloPacketReceived.future }

    // GLES2.DEPTH_TEST
    gl.depthFunc(GLES2.LESS)

    // GLES2.CULL_FACE
    gl.cullFace(GLES2.BACK)
    gl.frontFace(GLES2.CCW)

    gl.clearColor(0.75f, 0.75f, 0.75f, 1f) // Grey background

    val width = gl.display.width
    val height = gl.display.height

    screenDim = (width, height)
    Rendering.setProjection(width, height)

    networkFuture // wait for network setup (last part) to complete before proceding
  }

  def onDraw(fe: games.FrameEvent): Unit = {
    val now = System.currentTimeMillis()
    val elapsedSinceLastFrame = fe.elapsedTime

    val width = gl.display.width
    val height = gl.display.height

    val delta = mouse.deltaMotion

    var bulletShot = false

    val currentVelocity = new Vector2f
    var changeOrientation = 0f

    def processKeyboard() {
      val optKeyEvent = keyboard.nextEvent()
      for (keyEvent <- optKeyEvent) {
        if (keyEvent.down) {
          val key = keyEvent.key

          if (key == layout.mouseLock) mouse.locked = !mouse.locked
          else if (key == layout.fullscreen) gl.display.fullscreen = !gl.display.fullscreen
          else if (key == layout.volumeIncrease) {
            val newVolume = audioContext.volume + 0.05f
            audioContext.volume = newVolume
            Console.println("Volume increased to " + newVolume)
          } else if (key == layout.volumeDecrease) {
            val newVolume = Math.max(0f, audioContext.volume - 0.05f)
            audioContext.volume = newVolume
            Console.println("Volume decreased to " + newVolume)
          } else if (key == layout.changeLayout) {
            if (layout == Qwerty) {
              Console.println("Changing keyboard layout for Azerty")
              layout = Azerty
            } else {
              Console.println("Changing keyboard layout for Qwerty")
              layout = Qwerty
            }
          } else if (key == layout.escape) continueCond = false
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

    for (touchscreen <- this.touchscreen) {
      def processTouch() {
        val optTouchEvent = touchscreen.nextEvent()
        for (touchEvent <- optTouchEvent) {
          val touch = touchEvent.data

          if (touchEvent.start) {
            if (touch.position.y < height / 4) { // Config
              if (touch.position.x < width / 2) {
                gl.display.fullscreen = !gl.display.fullscreen
              } else {
                // TODO place for another action
              }
            } else { // Control
              if (touch.position.x < width / 2) { // Move
                if (this.moveTouch.isEmpty) this.moveTouch = Some(touch.identifier -> touch.position)
              } else { // Orientation
                if (this.orientationTouch.isEmpty) this.orientationTouch = Some(touch.identifier -> touch.position)
              }
              this.timeTouches += (touch.identifier -> now)
            }

          } else {
            for (time <- this.timeTouches.get(touch.identifier)) {
              if ((now - time) < maxTouchTimeToShootMS) {
                bulletShot = true
              }
            }
            for ((id, _) <- this.moveTouch) if (id == touch.identifier) this.moveTouch = None
            for ((id, _) <- this.orientationTouch) if (id == touch.identifier) this.orientationTouch = None
            this.timeTouches -= touch.identifier
          }

          processTouch() // process next event
        }
      }
      processTouch()

      val refSize = Math.max(width, height).toFloat

      for ((identifier, position) <- this.moveTouch) touchscreen.touches.find { _.identifier == identifier } match {
        case Some(touch) =>
          val originalPosition = position
          val currentPosition = touch.position

          val screenSizeFactorForMaxSpeed: Float = 12

          currentVelocity.x += (currentPosition.x - originalPosition.x).toFloat * screenSizeFactorForMaxSpeed / refSize * maxLateralSpeed
          if (currentPosition.y < originalPosition.y) currentVelocity.y += (currentPosition.y - originalPosition.y).toFloat / refSize * screenSizeFactorForMaxSpeed * maxForwardSpeed
          if (currentPosition.y > originalPosition.y) currentVelocity.y += (currentPosition.y - originalPosition.y).toFloat / refSize * screenSizeFactorForMaxSpeed * maxBackwardSpeed

        case None => this.moveTouch = None
      }

      for ((identifier, position) <- this.orientationTouch) touchscreen.touches.find { _.identifier == identifier } match {
        case Some(touch) =>
          val previousPosition = position
          val currentPosition = touch.position

          changeOrientation += ((currentPosition.x - previousPosition.x).toFloat / refSize) * -300f

          this.orientationTouch = Some(identifier -> currentPosition)

        case None => this.orientationTouch = None
      }
    }

    if (keyboard.isKeyDown(layout.forward)) currentVelocity.y += -maxForwardSpeed
    if (keyboard.isKeyDown(layout.backward)) currentVelocity.y += maxBackwardSpeed
    if (keyboard.isKeyDown(layout.right)) currentVelocity.x += maxLateralSpeed
    if (keyboard.isKeyDown(layout.left)) currentVelocity.x += -maxLateralSpeed

    changeOrientation += (delta.x.toFloat / width.toFloat) * -200f

    if (Math.abs(currentVelocity.x) > maxLateralSpeed) currentVelocity.x = Math.signum(currentVelocity.x) * maxLateralSpeed
    if (currentVelocity.y < -maxForwardSpeed) currentVelocity.y = -maxForwardSpeed
    if (currentVelocity.y > maxBackwardSpeed) currentVelocity.y = maxBackwardSpeed

    ifPresent { present =>
      val orientation = present.orientation + changeOrientation

      present.velocity = currentVelocity
      present.orientation = Physics.angleCentered(orientation)
    }

    val otherActivePlayers = externalPlayersState.flatMap {
      case (playerId, state: Present) => Some(playerId, state)
      case _                          => None
    }
    val activePlayers = otherActivePlayers ++ ifPresent { present => (this.localPlayerId, present) }

    //#### Simulation

    // Players
    for (
      (playerId, playerPresent) <- activePlayers
    ) {
      Physics.playerStep(playerPresent, elapsedSinceLastFrame)
    }

    // Projectiles
    this.projectiles = this.projectiles.filter { projWithId =>
      val (shooterId, projectile) = projWithId
      val ret = Physics.projectileStep(projWithId, activePlayers, elapsedSinceLastFrame)
      if (ret > 0 && shooterId == this.localPlayerId) for (conn <- connection) {
        val hit = network.ClientProjectileHit(projectile.id, ret)
        sendMsg(hit)

        this.lastTimeHit = Some(now)
      }
      ret < 0
    }

    //#### Network
    for (conn <- connection) {
      ifPresent { present =>
        val uPosition = Misc.conv(present.position)
        val uVelocity = Misc.conv(present.velocity)
        val uOrientation = present.orientation

        if (bulletShot && (lastTimeProjectileShot.isEmpty || now - lastTimeProjectileShot.get > shotIntervalMs)) {
          this.projectiles += (this.localPlayerId -> new Projectile(this.nextProjectileId, present.position.copy(), present.orientation))
          val shot = network.ClientProjectileShot(this.nextProjectileId, uPosition, uOrientation)
          sendMsg(shot)

          this.lastTimeProjectileShot = Some(now)
          this.nextProjectileId += 1

          val audioPlayer = this.audioShootData.attachNow(this.audioSimpleSource)
          audioPlayer.playing = true
          this.audioPlayers += audioPlayer
        }
      }

      if (lastTimeUpdateToServer.isEmpty || now - lastTimeUpdateToServer.get > updateIntervalMs) {
        val update = network.ClientUpdate(Misc.conv(this.localPlayerState))
        sendMsg(update)

        this.lastTimeUpdateToServer = Some(now)
      }
    }

    //#### Audio
    // Camera data
    val (camPosition, camOrientation) = ifPresent { present =>
      (present.position, present.orientation)
    }.getOrElse {
      val startingPosition = this.map.startPositions(localPlayerId).center.copy()
      val startingOrientation = this.map.startOrientations(localPlayerId)
      (startingPosition, startingOrientation)
    }

    val cameraRotationMatrix = Matrix3f.rotate3D(camOrientation, Vector3f.Up)
    val cameraPosition = new Vector3f(camPosition.x, Map.roomHalfSize, camPosition.y)
    val cameraTranslationMatrix = Matrix4f.translate3D(cameraPosition)

    val cameraTransform = cameraTranslationMatrix * cameraRotationMatrix.toHomogeneous()
    val cameraTransformInv = cameraTransform.invertedCopy()

    val audioListener = this.audioContext.listener
    audioListener.setOrientation(cameraRotationMatrix * Vector3f.Front, Vector3f.Up)
    audioListener.position = cameraPosition

    // Close and remove sounds that are over
    this.audioPlayers = this.audioPlayers.filter { player =>
      val playing = player.playing
      if (!playing) player.close()
      playing
    }

    // Update the position of the 3d sources
    for ((playerId, playerState) <- otherActivePlayers) {
      val source3d = getOrCreateSource3D(playerId)

      val position2d = playerState.position
      val position3d = new Vector3f(position2d.x, Map.roomHalfSize, position2d.y)
      source3d.position = position3d
    }

    //#### Graphics
    val curDim = (width, height)
    if (curDim != screenDim) {
      screenDim = curDim
      Rendering.setProjection(width, height)
    }

    // Clear the buffers
    gl.clear(GLES2.COLOR_BUFFER_BIT | GLES2.DEPTH_BUFFER_BIT)

    // 3D rendering
    gl.enable(GLES2.DEPTH_TEST)
    gl.enable(GLES2.CULL_FACE)

    Rendering.Map.init()
    Rendering.Map.render(cameraTransformInv)
    Rendering.Map.close()

    Rendering.Standard.init()
    // Render players
    for (
      (playerId, playerPresent) <- activePlayers if (playerId != localPlayerId)
    ) {
      val transform = Matrix4f.translate3D(new Vector3f(playerPresent.position.x, Map.roomHalfSize, playerPresent.position.y)) * Matrix4f.scale3D(new Vector3f(1, 1, 1) * 0.5f) * Matrix4f.rotate3D(playerPresent.orientation, Vector3f.Up)
      Rendering.Standard.render(playerId, Rendering.Player.mesh, transform, cameraTransformInv)
    }

    // Render projectiles
    for ((playerId, projectile) <- this.projectiles) {
      val transform = Matrix4f.translate3D(new Vector3f(projectile.position.x, Map.roomHalfSize, projectile.position.y)) * Matrix4f.scale3D(new Vector3f(1, 1, 1) * 0.5f) * Matrix4f.rotate3D(projectile.orientation, Vector3f.Up)
      Rendering.Standard.render(playerId, Rendering.Bullet.mesh, transform, cameraTransformInv)
    }
    Rendering.Standard.close()

    // 2D rendering

    gl.disable(GLES2.DEPTH_TEST)
    gl.disable(GLES2.CULL_FACE)

    Rendering.Hud.init()
    Rendering.Hud.render(this.localPlayerId, width, height, ifPresent(_.health).getOrElse(0f), this.lastTimeHit.map { last => (now - last).toInt })
    Rendering.Hud.close()

    //#### Ending
    continueCond = continueCond && itf.continue()
  }
}
