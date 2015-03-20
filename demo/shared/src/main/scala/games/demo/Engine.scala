package games.demo

import transport.WebSocketUrl
import scala.concurrent.{ Future, ExecutionContext }
import games._
import games.math
import games.math.{ Vector3f, Vector4f, Matrix4f, MatrixStack }
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

case class OpenGLSubMesh(indicesBuffer: Token.Buffer, verticesCount: Int, ambientColor: Vector3f, diffuseColor: Vector3f)
case class OpenGLMesh(verticesBuffer: Token.Buffer, normalsBuffer: Token.Buffer, verticesCount: Int, subMeshes: Array[OpenGLSubMesh], transform: Matrix4f)

class Engine(itf: EngineInterface)(implicit ec: ExecutionContext) extends games.FrameListener {
  def context: games.opengl.GLES2 = gl

  private var continueCond = true

  private var gl: GLES2 = _
  private var audioContext: Context = _
  private var keyboard: Keyboard = _
  private var mouse: Mouse = _

  def continue(): Boolean = continueCond

  def onClose(): Unit = {
    itf.printLine("Closing...")
    itf.close()

    mouse.close()
    keyboard.close()
    audioContext.close()
    gl.close()
  }

  def onCreate(): Unit = {
    itf.printLine("Init...")
    this.gl = new GLES2Debug(itf.initGL()) // Enable automatic error checking
    this.audioContext = itf.initAudio()
    this.keyboard = itf.initKeyboard()
    this.mouse = itf.initMouse()

    audioContext.volume = 0.5f

    // Prepare shaders
    val vertexSource = """
      uniform mat4 projection;
      uniform mat4 modelView;
      uniform mat4 modelViewInvTr;
      
      attribute vec3 position;
      attribute vec3 normal;
      
      varying vec3 varNormal;
      varying vec3 varView;
      
      void main(void) {
        vec4 pos = modelView * vec4(position, 1.0);
        gl_Position = projection * pos;
        varNormal = normalize((modelViewInvTr * vec4(normal, 1.0)).xyz);
        varView = normalize(-pos.xyz);
      }
      """

    val fragmentSource = """
      #ifdef GL_ES
        precision mediump float;
      #endif
      
      uniform vec3 diffuseColor;
      
      varying vec3 varNormal;
      varying vec3 varView;
      
      void main(void) {
        gl_FragColor = vec4(diffuseColor * dot(normalize(varView), normalize(varNormal)), 1.0);
      }
      """

    program = gl.createProgram()

    val vertexShader = gl.createShader(GLES2.VERTEX_SHADER)
    gl.shaderSource(vertexShader, vertexSource)
    gl.compileShader(vertexShader)
    gl.attachShader(program, vertexShader)

    val fragmentShader = gl.createShader(GLES2.FRAGMENT_SHADER)
    gl.shaderSource(fragmentShader, fragmentSource)
    gl.compileShader(fragmentShader)
    gl.attachShader(program, fragmentShader)

    gl.linkProgram(program)
    gl.useProgram(program)

    positionAttrLoc = gl.getAttribLocation(program, "position")
    normalAttrLoc = gl.getAttribLocation(program, "normal")

    diffuseColorUniLoc = gl.getUniformLocation(program, "diffuseColor")
    projectionUniLoc = gl.getUniformLocation(program, "projection")
    modelViewUniLoc = gl.getUniformLocation(program, "modelView")
    modelViewInvTrUniLoc = gl.getUniformLocation(program, "modelViewInvTr")

    gl.clearColor(0.5f, 0.5f, 0.5f, 1) // grey background

    gl.enable(GLES2.DEPTH_TEST)
    gl.depthFunc(GLES2.LESS)

    val width = gl.display.width
    val height = gl.display.height

    dim = (width, height)
    gl.viewport(0, 0, width, height)
    projection = Matrix4f.perspective3D(fovy, width.toFloat / height.toFloat, near, far)
    transformStack = new MatrixStack(new Matrix4f)
    cameraTransform = Matrix4f.translate3D(new Vector3f(0, 0, 5)) * new Matrix4f

    // Load mesh
    val futureMeshObj = Utils.getTextDataFromResource(Resource("/games/demo/sphere.obj"))
    val futureMeshMtl = Utils.getTextDataFromResource(Resource("/games/demo/sphere.mtl"))

    val futureMesh = Future.sequence(Seq(futureMeshObj, futureMeshMtl))
    futureMesh.onSuccess {
      case Seq(meshObj, meshMtl) =>
        val objLines = Utils.lines(meshObj)
        val mtlLines = Utils.lines(meshMtl)

        val objs = SimpleOBJParser.parseOBJ(objLines, Map("sphere.mtl" -> mtlLines))
        val meshes = SimpleOBJParser.convOBJObjectToTriMesh(objs)
        val mesh = meshes("Sphere")

        val meshVerticesCount = mesh.vertices.length

        val verticesData = GLES2.createFloatBuffer(meshVerticesCount * 3)
        mesh.vertices.foreach { v => v.store(verticesData) }
        verticesData.flip()
        val verticesBuffer = gl.createBuffer()
        gl.bindBuffer(GLES2.ARRAY_BUFFER, verticesBuffer)
        gl.bufferData(GLES2.ARRAY_BUFFER, verticesData, GLES2.STATIC_DRAW)

        val normals = mesh.normals.get
        val normalsData = GLES2.createFloatBuffer(meshVerticesCount * 3); require(meshVerticesCount == normals.length)
        normals.foreach { v => v.store(normalsData) }
        normalsData.flip()
        val normalsBuffer = gl.createBuffer()
        gl.bindBuffer(GLES2.ARRAY_BUFFER, normalsBuffer)
        gl.bufferData(GLES2.ARRAY_BUFFER, normalsData, GLES2.STATIC_DRAW)

        val openGLSubMeshes = mesh.submeshes.map { submesh =>
          val tris = submesh.tris
          val submeshVerticesCount = tris.length * 3
          val indicesData = GLES2.createShortBuffer(submeshVerticesCount)
          tris.foreach {
            case (i0, i1, i2) =>
              indicesData.put(i0.toShort)
              indicesData.put(i1.toShort)
              indicesData.put(i2.toShort)
          }
          indicesData.flip()
          val indicesBuffer = gl.createBuffer()
          gl.bindBuffer(GLES2.ELEMENT_ARRAY_BUFFER, indicesBuffer)
          gl.bufferData(GLES2.ELEMENT_ARRAY_BUFFER, indicesData, GLES2.STATIC_DRAW)

          OpenGLSubMesh(indicesBuffer, submeshVerticesCount, submesh.material.get.ambientColor.get, submesh.material.get.diffuseColor.get)
        }

        val openGLMesh = OpenGLMesh(verticesBuffer, normalsBuffer, meshVerticesCount, openGLSubMeshes, new Matrix4f)
        this.meshes += openGLMesh

        itf.printLine("Loading done")
    }
    futureMesh.onFailure { case t => itf.printLine("Failed to load the mesh: " + t) }
  }

  var program: Token.Program = _
  var projection: Matrix4f = _
  var transformStack: MatrixStack[Matrix4f] = _
  var cameraTransform: Matrix4f = _

  var verticesBuffer: Token.Buffer = _
  var indicesBuffer: Token.Buffer = _

  var positionAttrLoc: Int = _
  var normalAttrLoc: Int = _
  var diffuseColorUniLoc: Token.UniformLocation = _
  var projectionUniLoc: Token.UniformLocation = _
  var modelViewUniLoc: Token.UniformLocation = _
  var modelViewInvTrUniLoc: Token.UniformLocation = _

  val meshes = new ArrayBuffer[OpenGLMesh]()

  val fovy: Float = 70f
  val near: Float = 0.1f
  val far: Float = 100f

  val lookTranslationSpeed: Float = 2.0f

  var dim: (Int, Int) = _

  val sampleRate = 22100
  var audioSources: List[AbstractSource] = Nil

  def createMonoSound(freq: Int): ByteBuffer = {
    val bb = ByteBuffer.allocate(4 * sampleRate).order(ByteOrder.nativeOrder())

    var i = 0
    while (i < sampleRate) {
      val current = Math.sin(2 * Math.PI * freq * i.toDouble / sampleRate).toFloat
      bb.putFloat(current)
      i += 1
    }

    bb.rewind()
    bb
  }

  def onDraw(fe: games.FrameEvent): Unit = {
    def processKeyboard() {
      val event = keyboard.nextEvent()
      event match {
        case Some(KeyboardEvent(key, down)) => {
          if (down) key match {
            case Key.L => {
              mouse.locked = !mouse.locked
              itf.printLine("Pointer lock toggled")
            }
            case Key.Escape => continueCond = false
            case Key.F => {
              gl.display.fullscreen = !gl.display.fullscreen
              itf.printLine("Fullscreen toggled")
            }
            case Key.NumAdd => {
              audioContext.volume *= 1.25f
              itf.printLine("Increased volume to " + audioContext.volume)
            }
            case Key.NumSubstract => {
              audioContext.volume /= 1.25f
              itf.printLine("Decreased volume to " + audioContext.volume)
            }
            case Key.M => {
              if (audioSources.isEmpty) {
                //val data = audioContext.createBufferedData(Resource("/games/demo/test_mono.ogg"))
                val data = audioContext.createRawData(createMonoSound(1000), Format.Float32, 1, sampleRate)
                val source = data.createSource3D
                source.onSuccess {
                  case s =>
                    audioSources = s :: audioSources
                    s.loop = true
                    s.position = new Vector3f(0, 0, 0)
                    s.volume = 0.5f
                    s.play
                    itf.printLine("Adding audio source")
                }
                source.onFailure {
                  case t =>
                    itf.printLine("Could not load the sound: " + t)
                }
              } else {
                audioSources.foreach { source => source.close() }
                audioSources = Nil
                itf.printLine("Closing audio sources")
              }
            }
            case _ => // nothing to do
          }
          processKeyboard()
        }
        case None => // nothing to do
      }
    }
    processKeyboard()

    val width = gl.display.width
    val height = gl.display.height

    val curDim = (width, height)

    if (curDim != dim) {
      dim = curDim
      gl.viewport(0, 0, width, height)
      Matrix4f.setPerspective3D(fovy, width.toFloat / height.toFloat, near, far, projection)
    }

    var transX: Float = 0
    var transY: Float = 0
    if (keyboard.isKeyDown(Key.D)) transX += fe.elapsedTime * +lookTranslationSpeed
    if (keyboard.isKeyDown(Key.A)) transX += fe.elapsedTime * -lookTranslationSpeed
    if (keyboard.isKeyDown(Key.W)) transY += fe.elapsedTime * -lookTranslationSpeed
    if (keyboard.isKeyDown(Key.S)) transY += fe.elapsedTime * +lookTranslationSpeed
    cameraTransform = Matrix4f.translate3D(new Vector3f(transX, 0, transY)) * cameraTransform

    gl.clear(GLES2.COLOR_BUFFER_BIT | GLES2.DEPTH_BUFFER_BIT)

    gl.useProgram(program)

    gl.uniformMatrix4f(projectionUniLoc, projection)
    val cameraTransformInv = cameraTransform.invertedCopy()

    gl.enableVertexAttribArray(positionAttrLoc)
    gl.enableVertexAttribArray(normalAttrLoc)

    this.meshes.foreach { mesh =>
      val modelView = cameraTransformInv * mesh.transform
      val modelViewInvTr = modelView.invertedCopy().transpose()
      gl.uniformMatrix4f(modelViewUniLoc, modelView)
      gl.uniformMatrix4f(modelViewInvTrUniLoc, modelViewInvTr)

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

    continueCond = continueCond && itf.update()
  }
}
