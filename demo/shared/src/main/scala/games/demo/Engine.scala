package games.demo

import transport.WebSocketUrl
import scala.concurrent.{ Future, ExecutionContext }
import games._
import games.math
import games.math.Vector3f
import games.opengl._
import games.audio._
import games.input._
import games.utils._
import java.nio.{ ByteBuffer, FloatBuffer, ByteOrder }
import games.opengl.GLES2Debug
import games.audio.Source3D
import games.input.ButtonEvent
import games.audio.AbstractSource

abstract class EngineInterface {
  def printLine(msg: String): Unit
  def getScreenDim(): (Int, Int)
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
      attribute vec3 position;
      
      void main(void) {
        gl_Position = vec4(position, 1.0);
      }
      """

    val fragmentSource = """
      #ifdef GL_ES
        precision mediump float;
      #endif
      
      uniform vec3 color;
      
      void main(void) {
        gl_FragColor = vec4(color, 1.0);
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

    positionAttribLocation = gl.getAttribLocation(program, "position")
    colorUniformLocation = gl.getUniformLocation(program, "color")

    // Prepare data
    val verticesBufferData = GLES2.createFloatBuffer(3 * 3)
    verticesBufferData.put(-0.2f).put(-0.2f).put(0)
    verticesBufferData.put(0.2f).put(-0.2f).put(0)
    verticesBufferData.put(0).put(0.2f).put(0)
    verticesBufferData.rewind
    verticesBuffer = gl.createBuffer()
    gl.bindBuffer(GLES2.ARRAY_BUFFER, verticesBuffer)
    gl.bufferData(GLES2.ARRAY_BUFFER, verticesBufferData, GLES2.STATIC_DRAW)
    gl.vertexAttribPointer(positionAttribLocation, 3, GLES2.FLOAT, false, 3 * 4, 0) // 3 vertex, each vertex is 3 floats of 4 bytes

    val indicesBufferData = GLES2.createShortBuffer(3 * 1)
    indicesBufferData.put(0.toShort).put(1.toShort).put(2.toShort)
    indicesBufferData.rewind
    indicesBuffer = gl.createBuffer
    gl.bindBuffer(GLES2.ELEMENT_ARRAY_BUFFER, indicesBuffer)
    gl.bufferData(GLES2.ELEMENT_ARRAY_BUFFER, indicesBufferData, GLES2.STATIC_DRAW)

    gl.clearColor(1, 0, 0, 1) // red background

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

        val verticesData = GLES2.createFloatBuffer(mesh.vertices.length * 3)
        mesh.vertices.foreach { v => v.store(verticesData) }
        verticesData.rewind()
        val verticesBuffer = gl.createBuffer()
        gl.bindBuffer(GLES2.ARRAY_BUFFER, verticesBuffer)
        gl.bufferData(GLES2.ARRAY_BUFFER, verticesData, GLES2.STATIC_DRAW)

        val normals = mesh.normals.get
        val normalsData = GLES2.createFloatBuffer(normals.length * 3)
        normals.foreach { v => v.store(normalsData) }
        normalsData.rewind()
        val normalsBuffer = gl.createBuffer()
        gl.bindBuffer(GLES2.ARRAY_BUFFER, normalsBuffer)
        gl.bufferData(GLES2.ARRAY_BUFFER, normalsData, GLES2.STATIC_DRAW)

        mesh.submeshes.foreach { submesh =>
          val tris = submesh.tris
          val indicesData = GLES2.createShortBuffer(tris.length * 3)
          tris.foreach {
            case (i0, i1, i2) =>
              indicesData.put(i0.toShort)
              indicesData.put(i1.toShort)
              indicesData.put(i2.toShort)
          }
          indicesData.rewind()
          val indicesBuffer = gl.createBuffer()
          gl.bindBuffer(GLES2.ELEMENT_ARRAY_BUFFER, indicesBuffer)
          gl.bufferData(GLES2.ELEMENT_ARRAY_BUFFER, indicesData, GLES2.STATIC_DRAW)
        }

        itf.printLine("Loading done")
    }
    futureMesh.onFailure { case t => itf.printLine("Failed to load the mesh: " + t) }
  }

  var program: Token.Program = _

  var verticesBuffer: Token.Buffer = _
  var indicesBuffer: Token.Buffer = _
  var positionAttribLocation: Int = _
  var colorUniformLocation: Token.UniformLocation = _
  val triangleColor1 = new math.Vector3f(0, 0, 1)
  val triangleColor2 = new math.Vector3f(0, 1, 0)

  val sampleRate = 22100

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

  var audioSources: List[AbstractSource] = Nil

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

    val (width, height) = (gl.display.width, gl.display.height)

    gl.viewport(0, 0, width, height)

    gl.clear(GLES2.COLOR_BUFFER_BIT | GLES2.DEPTH_BUFFER_BIT)

    gl.useProgram(program)

    gl.uniform3f(colorUniformLocation, if (keyboard.isKeyDown(Key.Space)) triangleColor1 else triangleColor2)

    gl.enableVertexAttribArray(positionAttribLocation)
    gl.bindBuffer(GLES2.ELEMENT_ARRAY_BUFFER, indicesBuffer)
    gl.drawElements(GLES2.TRIANGLES, 3, GLES2.UNSIGNED_SHORT, 0)
    gl.disableVertexAttribArray(positionAttribLocation)

    continueCond = continueCond && itf.update()
  }
}
