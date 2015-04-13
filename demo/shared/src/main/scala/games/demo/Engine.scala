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

import scala.collection.immutable
import scala.collection.mutable

abstract class EngineInterface {
  def printLine(msg: String): Unit
  def initGL(): GLES2
  def initAudio(): Context
  def initKeyboard(): Keyboard
  def initMouse(): Mouse
  def update(): Boolean
  def close(): Unit
}

class PlayerData(var position: Vector3f, var velocity: Float, var orientation: Vector3f)
class ExternalPlayerData(var id: Int, var data: PlayerData, var rotation: Vector3f, var latency: Int)

class Engine(itf: EngineInterface)(implicit ec: ExecutionContext) extends games.FrameListener {
  private val updateIntervalMs = 25 // Resend position at 40Hz
  private val rotationSpeed: Float = 50.0f

  private val fovy: Float = 60f

  // render between 1cm and 100m
  private val near: Float = 0.01f
  private val far: Float = 100f

  def context: games.opengl.GLES2 = gl

  private var continueCond = true

  private var gl: GLES2 = _
  private var audioContext: Context = _
  private var keyboard: Keyboard = _
  private var mouse: Mouse = _

  private var connection: Option[ConnectionHandle] = None
  private var localPlayerId: Int = 0

  private var screenDim: (Int, Int) = _
  private var projection: Matrix4f = _

  private var localData: PlayerData = new PlayerData(new Vector3f, 0f, new Vector3f)
  private var extData: Seq[ExternalPlayerData] = Seq()

  private var lastTimeUpdateFromServer: Option[Long] = None
  private var lastTimeUpdateToServer: Option[Long] = None

  private var shipModel: OpenGLMesh = _
  private var shipProgram: Token.Program = _

  private var positionAttrLoc: Int = _
  private var normalAttrLoc: Int = _
  private var diffuseColorUniLoc: Token.UniformLocation = _
  private var projectionUniLoc: Token.UniformLocation = _
  private var modelViewUniLoc: Token.UniformLocation = _
  private var modelViewInvTrUniLoc: Token.UniformLocation = _

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

  def onCreate(): Option[Future[Unit]] = {
    itf.printLine("Starting...")
    this.gl = new GLES2Debug(itf.initGL()) // Init OpenGL (Enable automatic error checking by encapsuling it in GLES2Debug)
    this.audioContext = itf.initAudio() // Init Audio
    this.keyboard = itf.initKeyboard() // Init Keyboard listening
    this.mouse = itf.initMouse() // Init Mouse listener

    audioContext.volume = 0.25f // Lower the initial global volume

    // Loading data
    val modelsFuture = Rendering.loadAllModels("/games/demo/models", gl, loopExecutionContext)
    val shadersFuture = Rendering.loadAllShaders("/games/demo/shaders", gl, loopExecutionContext)

    val dataLoadedFuture = for (
      models <- modelsFuture;
      shaders <- shadersFuture
    ) yield {
      itf.printLine("All data loaded successfully: " + models.size + " model(s), " + shaders.size + " shader(s)")

      shipModel = models("ship")
      shipProgram = shaders("ship")
    }

    // Retrieve useful data from shaders (require access to OpenGL context)
    val retrieveInfoFromDataFuture = dataLoadedFuture.map { _ =>
      positionAttrLoc = gl.getAttribLocation(shipProgram, "position")
      normalAttrLoc = gl.getAttribLocation(shipProgram, "normal")

      diffuseColorUniLoc = gl.getUniformLocation(shipProgram, "diffuseColor")
      projectionUniLoc = gl.getUniformLocation(shipProgram, "projection")
      modelViewUniLoc = gl.getUniformLocation(shipProgram, "modelView")
      modelViewInvTrUniLoc = gl.getUniformLocation(shipProgram, "modelViewInvTr")
    }(loopExecutionContext)

    // Init network (wait for data loading to complete before that)
    val networkStartedFuture = retrieveInfoFromDataFuture.flatMap { _ =>
      val futureConnection = new WebSocketClient().connect(WebSocketUrl(Data.server))
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

              case Hello(playerId, initPostion, initOrientation) =>
                this.connection = Some(conn)
                localPlayerId = playerId
                localData.position = conv(initPostion)
                localData.orientation = conv(initOrientation)
                itf.printLine("You are player " + playerId)

              case ServerUpdate(players, newEvents) =>
                lastTimeUpdateFromServer = Some(now)

                val (locals, externals) = players.partition(_.id == localPlayerId)

                assert(locals.size == 1)
                val local = locals.head

                extData = externals.map { data =>
                  new ExternalPlayerData(data.id, new PlayerData(conv(data.position), data.velocity, conv(data.orientation)), conv(data.rotation), data.latency + local.latency)
                }
                newEvents.foreach { event =>
                  // TODO process event
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
    }

    // Setup OpenGL
    gl.clearColor(0.75f, 0.75f, 0.75f, 1) // black background

    gl.enable(GLES2.DEPTH_TEST)
    gl.depthFunc(GLES2.LESS)

    val width = gl.display.width
    val height = gl.display.height

    screenDim = (width, height)

    gl.viewport(0, 0, width, height)
    projection = Matrix4f.perspective3D(fovy, width.toFloat / height.toFloat, near, far)

    Some(networkStartedFuture) // wait for network setup (last part) to complete before proceding
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
          case Key.L      => mouse.locked = !mouse.locked
          case Key.F      => gl.display.fullscreen = !gl.display.fullscreen
          case _          => // nothing to do
        }

        processKeyboard() // process next event
      }
    }
    processKeyboard()

    if (keyboard.isKeyDown(Key.W)) localData.velocity = 3f
    else if (keyboard.isKeyDown(Key.S)) localData.velocity = 1f
    else localData.velocity = 2f

    // Simulation
    localData.orientation.x += (delta.x.toFloat / width.toFloat) * -rotationSpeed
    localData.orientation.y += (delta.y.toFloat / height.toFloat) * -rotationSpeed

    val localOrientationMatrix = Physics.matrixForOrientation(localData.orientation)
    localData.position += localOrientationMatrix * (Vector3f.Front * (localData.velocity * elapsedSinceLastFrame))

    // Network (if necessary)
    for (conn <- connection) {
      if (lastTimeUpdateToServer.isEmpty || now - lastTimeUpdateToServer.get > updateIntervalMs) {
        val position = conv(localData.position)
        val velocity = localData.velocity
        val orientation = conv(localData.orientation)
        val rotation = Vector3(0, 0, 0)
        val clientUpdate = ClientUpdate(position, velocity, orientation, rotation)

        val msgText = upickle.write(clientUpdate)
        conn.write(msgText)

        lastTimeUpdateToServer = Some(now)
      }
    }

    // Rendering
    val curDim = (width, height)
    if (curDim != screenDim) {
      screenDim = curDim
      gl.viewport(0, 0, width, height)
      Matrix4f.setPerspective3D(fovy, width.toFloat / height.toFloat, near, far, projection)
    }

    gl.clear(GLES2.COLOR_BUFFER_BIT | GLES2.DEPTH_BUFFER_BIT)
    gl.useProgram(shipProgram)
    gl.uniformMatrix4f(projectionUniLoc, projection)

    gl.enableVertexAttribArray(positionAttrLoc)
    gl.enableVertexAttribArray(normalAttrLoc)

    val cameraTransform = Matrix4f.translate3D(localData.position) * localOrientationMatrix.toHomogeneous()
    val cameraTransformInv = cameraTransform.invertedCopy()

    for (otherPlayer <- extData) {
      val transform = Matrix4f.translate3D(otherPlayer.data.position) * Physics.matrixForOrientation(otherPlayer.data.orientation).toHomogeneous()
      val modelView = cameraTransformInv * transform
      val modelViewInvTr = modelView.invertedCopy().transpose()

      gl.uniformMatrix4f(modelViewUniLoc, modelView)
      gl.uniformMatrix4f(modelViewInvTrUniLoc, modelViewInvTr)

      val mesh = shipModel

      gl.bindBuffer(GLES2.ARRAY_BUFFER, mesh.verticesBuffer)
      gl.vertexAttribPointer(positionAttrLoc, 3, GLES2.FLOAT, false, 0, 0)
      gl.bindBuffer(GLES2.ARRAY_BUFFER, mesh.normalsBuffer)
      gl.vertexAttribPointer(normalAttrLoc, 3, GLES2.FLOAT, false, 0, 0)
      mesh.subMeshes.foreach { submesh =>
        gl.uniform3f(diffuseColorUniLoc, submesh.diffuseColor)
        gl.bindBuffer(GLES2.ELEMENT_ARRAY_BUFFER, submesh.indicesBuffer)
        gl.drawElements(GLES2.TRIANGLES, submesh.verticesCount, GLES2.UNSIGNED_SHORT, 0)
      }
    }

    gl.disableVertexAttribArray(normalAttrLoc)
    gl.disableVertexAttribArray(positionAttrLoc)

    // Ending
    continueCond = continueCond && itf.update()
  }
}
