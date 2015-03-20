package games.demo

import transport.WebSocketUrl
import scala.concurrent.ExecutionContext

import games._
import games.math
import games.math.Vector3f
import games.opengl._
import games.audio._
import games.input._

import java.nio.{ ByteBuffer, FloatBuffer, ByteOrder }

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
          itf.printLine("Key " + key + (if (down) " is down" else " is up"))

          if (down) key match {
            case Key.L      => mouse.locked = !mouse.locked
            case Key.Escape => continueCond = false
            case Key.F      => gl.display.fullscreen = !gl.display.fullscreen
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
                }
                source.onFailure {
                  case t =>
                    println("Could not load the sound: " + t)
                    t.printStackTrace()
                }
              } else {
                audioSources.foreach { source => source.close() }
                audioSources = Nil
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

    def processMouse() {
      val event = mouse.nextEvent()
      event match {
        case Some(WheelEvent(wheel)) => {
          itf.printLine("Wheel rotated " + wheel)
        }
        case Some(ButtonEvent(button, down)) => {
          itf.printLine("Button " + button + (if (down) " is down" else " is up"))

          if (down) button match {
            //case Button.Left => itf.printLine("Mouse is at " + mouse.position.x + "x" + mouse.position.y + " (display is " + gl.display.width + "x" + gl.display.height + ")")
            case _ => // nothing to do
          }

          processMouse()
        }
        case None => // nothing to do
      }
    }
    processMouse()

    val (width, height) = (gl.display.width, gl.display.height)

    if (mouse.isButtonDown(Button.Left)) {
      def interpol(curIn: Float, maxIn: Float, startValue: Float, endValue: Float): Float = (startValue + curIn / maxIn * (endValue - startValue))

      val mousePos = mouse.position
      val startValue = -10.0
      val endValue = 10.0
      val posX = interpol(mousePos.x, width, -10, 10)
      val posY = interpol(mousePos.y, height, -10, 10)
      audioSources.foreach {
        case s: Source3D => s.position = new Vector3f(posX, 0, posY)
        case _           =>
      }
    }

    //val (width, height) = itf.getScreenDim()

    gl.viewport(0, 0, width, height)

    gl.clear(GLES2.COLOR_BUFFER_BIT)

    gl.useProgram(program)

    gl.uniform3f(colorUniformLocation, if (keyboard.isKeyDown(Key.Space)) triangleColor1 else triangleColor2)

    gl.enableVertexAttribArray(positionAttribLocation)
    gl.bindBuffer(GLES2.ELEMENT_ARRAY_BUFFER, indicesBuffer)
    gl.drawElements(GLES2.TRIANGLES, 3, GLES2.UNSIGNED_SHORT, 0)
    gl.disableVertexAttribArray(positionAttribLocation)

    continueCond = continueCond && itf.update()
  }
}
