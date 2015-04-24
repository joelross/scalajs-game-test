package games.opengl

import java.nio.{ Buffer, ByteBuffer, ShortBuffer, IntBuffer, FloatBuffer, DoubleBuffer, ByteOrder }

import org.scalajs.dom

import scala.scalajs.js
import js.Dynamic.{ global => g }

import scala.concurrent.Future

import games.JsUtils

//import scala.scalajs.js.typedarray.TypedArrayBufferOps
import scala.scalajs.js.typedarray.TypedArrayBufferOps._

// See https://github.com/scala-js/scala-js-dom/blob/master/src/main/scala/org/scalajs/dom/WebGL.scala for documentation
// about the WebGL DOM for ScalaJS

// Auxiliary components

object Token {

  type Buffer = org.scalajs.dom.raw.WebGLBuffer

  object Buffer {
    val invalid: Buffer = null
    val none: Buffer = null
  }

  type Program = org.scalajs.dom.raw.WebGLProgram

  object Program {
    val invalid: Program = null
  }

  type Shader = org.scalajs.dom.raw.WebGLShader

  object Shader {
    val invalid: Shader = null
  }

  type UniformLocation = org.scalajs.dom.raw.WebGLUniformLocation

  object UniformLocation {
    val invalid: UniformLocation = null
  }

  type FrameBuffer = org.scalajs.dom.raw.WebGLFramebuffer

  object FrameBuffer {
    val invalid: FrameBuffer = null
    val none: FrameBuffer = null
  }

  type RenderBuffer = org.scalajs.dom.raw.WebGLRenderbuffer

  object RenderBuffer {
    val invalid: RenderBuffer = null
    val none: RenderBuffer = null
  }

  type Texture = org.scalajs.dom.raw.WebGLTexture

  object Texture {
    val invalid: Texture = null
    val none: Texture = null
  }

}

// Main componenents

class DisplayGLES2(gl: GLES2WebGL) extends Display {

  private var fullscreenRequested = false

  private val onFullscreenChange: js.Function = (e: js.Dynamic) => {
    if (!this.fullscreen) {
      canvas.width = canvasPrevDim._1
      canvas.height = canvasPrevDim._2
    } else {
      // Necessary for the size of the canvas (Chrome) and its resolution (Firefox)
      canvas.width = screen.width
      canvas.height = screen.height

      if (JsUtils.orientationLockOnFullscreen) {
        games.input.AccelerometerJS.lockOrientation(games.input.AccelerometerJS.currentOrientation())
      }
    }

    if (JsUtils.autoToggling) this.fullscreen = fullscreenRequested // If the fullscreen state has changed against the wish of the user, change back ASAP
  }
  private val onFullscreenError: js.Function = (e: js.Dynamic) => {
    // nothing to do?
    js.Dynamic.global.console.log("onFullscreenError", this.fullscreen, e)
  }

  private val onOrientationChange: js.Function = (e: js.Dynamic) => {
    if (this.fullscreen) { // Update the canvas size in case of fullscreen mode
      /*
       * Wait 1000 ms before reading the screen dimension
       * (Firefox has not yet applied the orientation change, and is a bit
       * slow to do it)
       */
      val delay = 1000
      js.Dynamic.global.setTimeout(() => {
        if (this.fullscreen) { // check again (may have change it the mean time)
          canvas.width = screen.width
          canvas.height = screen.height
        }
      }, delay)
    }
  }

  private val canvas = gl.getWebGLRenderingContext().canvas.asInstanceOf[js.Dynamic]
  private val document = dom.document.asInstanceOf[js.Dynamic]
  private def window = js.Dynamic.global.window
  private def screen = js.Dynamic.global.screen

  // Init
  {
    val fullscreenRequest = JsUtils.getOptional[js.Dynamic](canvas, "requestFullscreen", "webkitRequestFullscreen", "mozRequestFullscreen", "mozRequestFullScreen")
    val fullscreenExit = JsUtils.getOptional[js.Dynamic](document, "exitFullscreen", "webkitCancelFullScreen", "mozCancelFullScreen")

    canvas.fullscreenRequest = fullscreenRequest.getOrElse(JsUtils.featureUnsupportedFunction("Fullscreen (Request)"))
    document.fullscreenExit = fullscreenExit.getOrElse(JsUtils.featureUnsupportedFunction("Fullscreen (Exit)"))

    document.addEventListener("fullscreenchange", onFullscreenChange, true)
    document.addEventListener("webkitfullscreenchange", onFullscreenChange, true)
    document.addEventListener("mozfullscreenchange", onFullscreenChange, true)
    document.addEventListener("MSFullscreenChange", onFullscreenChange, true)

    document.addEventListener("fullscreenerror", onFullscreenError, true)
    document.addEventListener("webkitfullscreenerror", onFullscreenError, true)
    document.addEventListener("mozfullscreenerror", onFullscreenError, true)
    document.addEventListener("MSFullscreenError", onFullscreenError, true)

    // What about this one? https://w3c.github.io/screen-orientation/#widl-ScreenOrientation-onchange
    window.addEventListener("orientationchange", onOrientationChange, true)
    if (JsUtils.getOptional[js.Function](screen, "addEventListener").isDefined) {
      screen.addEventListener("orientationchange", onOrientationChange, true)
      screen.addEventListener("mozorientationchange", onOrientationChange, true)
    }
    document.addEventListener("orientationchange", onOrientationChange, true)
  }

  private var canvasPrevDim: (Int, Int) = (canvas.width.asInstanceOf[Int], canvas.height.asInstanceOf[Int])

  def fullscreen: Boolean = {
    //val isFullscreen = JsUtils.getOptional[Boolean](document, "webkitIsFullScreen", "mozFullScreen")
    val element = JsUtils.getOptional[js.Dynamic](document, "fullscreenElement", "webkitFullscreenElement", "mozFullScreenElement")
    element match {
      case Some(el) => el == canvas
      case None     => false
    }
  }
  def fullscreen_=(fullscreen: Boolean): Unit = {
    fullscreenRequested = fullscreen // Remember the choice of the user

    val currentlyFullscreen = this.fullscreen

    if (fullscreen && !currentlyFullscreen) {
      canvasPrevDim = (canvas.width.asInstanceOf[Int], canvas.height.asInstanceOf[Int])
      Future {
        canvas.fullscreenRequest()
      }(JsUtils.userEventExecutionContext)
    } else if (!fullscreen && currentlyFullscreen) {
      Future {
        document.fullscreenExit()
      }(JsUtils.userEventExecutionContext)
    }
  }

  def width: Int = gl.getWebGLRenderingContext().drawingBufferWidth
  def height: Int = gl.getWebGLRenderingContext().drawingBufferHeight

  override def close(): Unit = {
    super.close()

    this.fullscreen = false

    document.removeEventListener("fullscreenchange", onFullscreenChange, true)
    document.removeEventListener("webkitfullscreenchange", onFullscreenChange, true)
    document.removeEventListener("mozfullscreenchange", onFullscreenChange, true)
    document.removeEventListener("MSFullscreenChange", onFullscreenChange, true)

    document.removeEventListener("fullscreenerror", onFullscreenError, true)
    document.removeEventListener("webkitfullscreenerror", onFullscreenError, true)
    document.removeEventListener("mozfullscreenerror", onFullscreenError, true)
    document.removeEventListener("MSFullscreenError", onFullscreenError, true)

    window.removeEventListener("orientationchange", onOrientationChange, true)
    if (JsUtils.getOptional[js.Function](screen, "removeEventListener").isDefined) {
      screen.removeEventListener("orientationchange", onOrientationChange, true)
      screen.removeEventListener("mozorientationchange", onOrientationChange, true)
    }
    document.removeEventListener("orientationchange", onOrientationChange, true)
  }
}

class GLES2WebGL(webGL: dom.raw.WebGLRenderingContext) extends GLES2 {
  def this(canvas: dom.html.Canvas) = {
    this((canvas.getContext("webgl").asInstanceOf[js.UndefOr[dom.raw.WebGLRenderingContext]]).orElse(canvas.getContext("experimental-webgl").asInstanceOf[js.UndefOr[dom.raw.WebGLRenderingContext]]).getOrElse(throw new RuntimeException("WebGL not supported by the browser")))
  }

  final val display: Display = new DisplayGLES2(this)

  override def close(): Unit = {
    super.close()
    display.close()
  }

  /* JS Specific */

  final def getWebGLRenderingContext(): dom.raw.WebGLRenderingContext = webGL

  /* public API */

  final def activeTexture(texture: Int): Unit = {
    webGL.activeTexture(texture)
  }

  final def attachShader(program: Token.Program, shader: Token.Shader): Unit = {
    webGL.attachShader(program, shader)
  }

  final def bindAttribLocation(program: Token.Program, index: Int, name: String): Unit = {
    webGL.bindAttribLocation(program, index, name)
  }

  final def bindBuffer(target: Int, buffer: Token.Buffer): Unit = {
    webGL.bindBuffer(target, buffer)
  }

  final def bindFramebuffer(target: Int, framebuffer: Token.FrameBuffer): Unit = {
    webGL.bindFramebuffer(target, framebuffer)
  }

  final def bindRenderbuffer(target: Int, renderbuffer: Token.RenderBuffer): Unit = {
    webGL.bindRenderbuffer(target, renderbuffer)
  }

  final def bindTexture(target: Int, texture: Token.Texture): Unit = {
    webGL.bindTexture(target, texture)
  }

  final def blendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = {
    webGL.blendColor(red, green, blue, alpha)
  }

  final def blendEquation(mode: Int): Unit = {
    webGL.blendEquation(mode)
  }

  final def blendEquationSeparate(modeRGB: Int, modeAlpha: Int): Unit = {
    webGL.blendEquationSeparate(modeRGB, modeAlpha)
  }

  final def blendFunc(sfactor: Int, dfactor: Int): Unit = {
    webGL.blendFunc(sfactor, dfactor)
  }

  final def blendFuncSeparate(srcfactorRGB: Int, dstfactorRGB: Int, srcfactorAlpha: Int, dstfactorAlpha: Int): Unit = {
    webGL.blendFuncSeparate(srcfactorRGB, dstfactorRGB, srcfactorAlpha, dstfactorAlpha)
  }

  final def bufferData(target: Int, totalBytes: Long, usage: Int): Unit = {
    webGL.bufferData(target, totalBytes.toInt, usage)
  }

  private final def _bufferData(target: Int, data: Buffer, usage: Int): Unit = {
    val buffer: Buffer = data
    if (buffer != null)
      require(buffer.hasArrayBuffer()) // should we have a backup plan?
    webGL.bufferData(target, if (buffer != null) buffer.dataView() else null, usage)
  }

  final def bufferData(target: Int, data: ByteBuffer, usage: Int): Unit = this._bufferData(target, if (data != null) data.slice else null, usage)
  final def bufferData(target: Int, data: ShortBuffer, usage: Int): Unit = this._bufferData(target, if (data != null) data.slice else null, usage)
  final def bufferData(target: Int, data: IntBuffer, usage: Int): Unit = this._bufferData(target, if (data != null) data.slice else null, usage)
  final def bufferData(target: Int, data: FloatBuffer, usage: Int): Unit = this._bufferData(target, if (data != null) data.slice else null, usage)
  final def bufferData(target: Int, data: DoubleBuffer, usage: Int): Unit = this._bufferData(target, if (data != null) data.slice else null, usage)

  private final def _bufferSubData(target: Int, offset: Long, data: Buffer): Unit = {
    // Not really how the Long is going to behave in JavaScript
    val buffer: Buffer = data
    if (buffer != null)
      require(buffer.hasArrayBuffer()) // should we have a backup plan?

    // TODO bufferSubData currently missing from org.scalajs.dom, correct this once it's ok
    // PS: bufferSubData exists in the WebGL specs
    webGL.asInstanceOf[js.Dynamic].bufferSubData(target, offset, if (buffer != null) buffer.dataView() else null)
  }

  final def bufferSubData(target: Int, offset: Long, data: ByteBuffer): Unit = this._bufferSubData(target, offset, if (data != null) data.slice else null)
  final def bufferSubData(target: Int, offset: Long, data: ShortBuffer): Unit = this._bufferSubData(target, offset, if (data != null) data.slice else null)
  final def bufferSubData(target: Int, offset: Long, data: IntBuffer): Unit = this._bufferSubData(target, offset, if (data != null) data.slice else null)
  final def bufferSubData(target: Int, offset: Long, data: FloatBuffer): Unit = this._bufferSubData(target, offset, if (data != null) data.slice else null)
  final def bufferSubData(target: Int, offset: Long, data: DoubleBuffer): Unit = this._bufferSubData(target, offset, if (data != null) data.slice else null)

  final def checkFramebufferStatus(target: Int): Int = {
    webGL.checkFramebufferStatus(target).toInt
  }

  final def clear(mask: Int): Unit = {
    webGL.clear(mask)
  }

  final def clearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = {
    webGL.clearColor(red, green, blue, alpha)
  }

  final def clearDepth(depth: Double): Unit = {
    webGL.asInstanceOf[js.Dynamic].clearDepth(depth)
    //gl.clearDepth(depth) // not correct in Scala dom
  }

  final def clearStencil(s: Int): Unit = {
    webGL.clearStencil(s)
  }

  final def colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit = {
    webGL.colorMask(red, green, blue, alpha)
  }

  final def compileShader(shader: Token.Shader): Unit = {
    webGL.compileShader(shader)
  }

  final def compressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                                 data: ByteBuffer): Unit = {

    val bytebuffer: ByteBuffer = if (data != null) { val tmp = data.slice; require(tmp.hasArrayBuffer()); tmp } else null
    webGL.compressedTexImage2D(target, level, internalformat, width, height, border, if (data != null) bytebuffer.dataView() else null)
  }

  final def compressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                                    format: Int, data: ByteBuffer): Unit = {

    val bytebuffer: ByteBuffer = if (data != null) { val tmp = data.slice; require(tmp.hasArrayBuffer()); tmp } else null
    webGL.compressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, if (data != null) bytebuffer.dataView() else null)
  }

  final def copyTexImage2D(target: Int, level: Int, internalFormat: Int, x: Int, y: Int, width: Int, height: Int, border: Int): Unit = {
    webGL.copyTexImage2D(target, level, internalFormat, x, y, width, height, border)
  }

  final def copyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit = {
    webGL.copyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)
  }

  final def createBuffer(): Token.Buffer = {
    val ret = webGL.createBuffer()
    ret
  }

  final def createFramebuffer(): Token.FrameBuffer = {
    webGL.createFramebuffer()
  }

  final def createProgram(): Token.Program = {
    webGL.createProgram()
  }

  final def createRenderbuffer(): Token.RenderBuffer = {
    // TODO org.scalajs.dom has the name wrong the 'b' is not capital in the WebGL spec, correct this once it's ok
    webGL.asInstanceOf[js.Dynamic].createRenderbuffer().asInstanceOf[Token.RenderBuffer]
  }

  final def createShader(`type`: Int): Token.Shader = {
    webGL.createShader(`type`)
  }

  final def createTexture(): Token.Texture = {
    webGL.createTexture()
  }

  final def cullFace(mode: Int): Unit = {
    webGL.cullFace(mode)
  }

  final def deleteBuffer(buffer: Token.Buffer): Unit = {
    webGL.deleteBuffer(buffer)
  }

  final def deleteFramebuffer(framebuffer: Token.FrameBuffer): Unit = {
    webGL.deleteFramebuffer(framebuffer)
  }

  final def deleteProgram(program: Token.Program): Unit = {
    webGL.deleteProgram(program)
  }

  final def deleteRenderbuffer(renderbuffer: Token.RenderBuffer): Unit = {
    webGL.deleteRenderbuffer(renderbuffer)
  }

  final def deleteShader(shader: Token.Shader): Unit = {
    webGL.deleteShader(shader)
  }

  final def deleteTexture(texture: Token.Texture): Unit = {
    webGL.deleteTexture(texture)
  }

  final def depthFunc(func: Int): Unit = {
    webGL.depthFunc(func)
  }

  final def depthMask(flag: Boolean): Unit = {
    webGL.depthMask(flag)
  }

  final def depthRange(zNear: Double, zFar: Double): Unit = {
    webGL.depthRange(zNear, zFar)
  }

  final def detachShader(program: Token.Program, shader: Token.Shader): Unit = {
    webGL.detachShader(program, shader)
  }

  final def disable(cap: Int): Unit = {
    webGL.disable(cap)
  }

  final def disableVertexAttribArray(index: Int): Unit = {
    webGL.disableVertexAttribArray(index)
  }

  final def drawArrays(mode: Int, first: Int, count: Int): Unit = {
    webGL.drawArrays(mode, first, count)
  }

  final def drawElements(mode: Int, count: Int, `type`: Int, offset: Long): Unit = {
    // may be a good idea to check that an element array buffer is currently bound
    webGL.drawElements(mode, count, `type`, offset.toInt)
  }

  final def enable(cap: Int): Unit = {
    webGL.enable(cap)
  }

  final def enableVertexAttribArray(index: Int): Unit = {
    webGL.enableVertexAttribArray(index)
  }

  final def finish(): Unit = {
    webGL.finish()
  }

  final def flush(): Unit = {
    webGL.flush()
  }

  final def framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Token.RenderBuffer): Unit = {
    webGL.framebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)
  }

  final def framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Token.Texture, level: Int): Unit = {
    webGL.framebufferTexture2D(target, attachment, textarget, texture, level)
  }

  final def frontFace(mode: Int): Unit = {
    webGL.frontFace(mode)
  }

  final def generateMipmap(target: Int): Unit = {
    webGL.generateMipmap(target)
  }

  final def getActiveAttrib(program: Token.Program, index: Int): ActiveInfo = {
    val jsActiveInfo = webGL.getActiveAttrib(program, index)
    ActiveInfo(jsActiveInfo.size.toInt, jsActiveInfo.`type`.toInt, jsActiveInfo.name)
  }

  final def getActiveUniform(program: Token.Program, index: Int): ActiveInfo = {
    // TODO org.scalajs.dom has the return type wrong, correct this once it's ok
    val jsActiveInfoDyn = webGL.asInstanceOf[js.Dynamic].getActiveUniform(program, index)
    val jsActiveInfo = jsActiveInfoDyn.asInstanceOf[dom.raw.WebGLActiveInfo]
    ActiveInfo(jsActiveInfo.size.toInt, jsActiveInfo.`type`.toInt, jsActiveInfo.name)
  }

  final def getAttachedShaders(program: Token.Program): Array[Token.Shader] = {
    val jsArray = webGL.getAttachedShaders(program)
    jsArray.toArray
  }

  final def getAttribLocation(program: Token.Program, name: String): Int = {
    webGL.getAttribLocation(program, name).toInt
  }

  final def getBufferParameteri(target: Int, pname: Int): Int = {
    // accept only GL_BUFFER_SIZE and GL_BUFFER_USAGE, both return a simple Int, no problem here
    webGL.getBufferParameter(target, pname).toInt
  }

  final def getParameterBuffer(pname: Int): Token.Buffer = {
    webGL.getParameter(pname).asInstanceOf[Token.Buffer]
  }

  final def getParameterTexture(pname: Int): Token.Texture = {
    webGL.getParameter(pname).asInstanceOf[Token.Texture]
  }

  final def getParameterFramebuffer(pname: Int): Token.FrameBuffer = {
    webGL.getParameter(pname).asInstanceOf[Token.FrameBuffer]
  }

  final def getParameterProgram(pname: Int): Token.Program = {
    webGL.getParameter(pname).asInstanceOf[Token.Program]
  }

  final def getParameterRenderbuffer(pname: Int): Token.RenderBuffer = {
    webGL.getParameter(pname).asInstanceOf[Token.RenderBuffer]
  }

  final def getParameterShader(pname: Int): Token.Shader = {
    webGL.getParameter(pname).asInstanceOf[Token.Shader]
  }

  final def getParameterString(pname: Int): String = {
    webGL.getParameter(pname).asInstanceOf[String]
  }

  final def getParameteri(pname: Int): Int = {
    // LWJGL hint: use glGetInteger(int pname): Int
    val ret = webGL.getParameter(pname)
    JSTypeHelper.toInt(ret)
  }

  final def getParameteriv(pname: Int, outputs: IntBuffer): Unit = {
    // LWJGL hint: use glGetInteger(int pname, IntBuffer params)
    val ret = webGL.getParameter(pname)
    JSTypeHelper.toInts(ret, outputs)
  }

  final def getParameterf(pname: Int): Float = {
    // LWJGL hint: use glGetFloat(int pname): Int
    webGL.getParameter(pname).asInstanceOf[Double].toFloat
  }

  final def getParameterfv(pname: Int, outputs: FloatBuffer): Unit = {
    // LWJGL hint: use glGetInteger(int pname, IntBuffer params)
    val ret = webGL.getParameter(pname)
    JSTypeHelper.toFloats(ret, outputs)
  }

  final def getParameterb(pname: Int): Boolean = {
    // LWJGL hint: use glGetBoolean(int pname): Boolean
    val ret = webGL.getParameter(pname)
    JSTypeHelper.toBoolean(ret)
  }

  final def getParameterbv(pname: Int, outputs: ByteBuffer): Unit = {
    // LWJGL hint: use glGetBoolean(int pname, ByteBuffer params)
    val ret = webGL.getParameter(pname)
    JSTypeHelper.toBooleans(ret, outputs)
  }

  final def getError(): Int = {
    webGL.getError().toInt
  }

  final def getFramebufferAttachmentParameteri(target: Int, attachment: Int, pname: Int): Int = {
    webGL.getFramebufferAttachmentParameter(target, attachment, pname).asInstanceOf[Double].toInt
  }

  final def getFramebufferAttachmentParameterRenderbuffer(target: Int, attachment: Int, pname: Int): Token.RenderBuffer = {
    webGL.getFramebufferAttachmentParameter(target, attachment, pname).asInstanceOf[Token.RenderBuffer]
  }

  final def getFramebufferAttachmentParameterTexture(target: Int, attachment: Int, pname: Int): Token.Texture = {
    webGL.getFramebufferAttachmentParameter(target, attachment, pname).asInstanceOf[Token.Texture]
  }

  final def getProgramParameteri(program: Token.Program, pname: Int): Int = {
    val ret = webGL.getProgramParameter(program, pname)
    JSTypeHelper.toInt(ret)
  }

  final def getProgramParameterb(program: Token.Program, pname: Int): Boolean = {
    val ret = webGL.getProgramParameter(program, pname)
    JSTypeHelper.toBoolean(ret)
  }

  final def getProgramInfoLog(program: Token.Program): String = {
    webGL.getProgramInfoLog(program)
  }

  final def getRenderbufferParameteri(target: Int, pname: Int): Int = {
    webGL.getRenderbufferParameter(target, pname).asInstanceOf[Double].toInt
  }

  final def getShaderParameteri(shader: Token.Shader, pname: Int): Int = {
    val ret = webGL.getShaderParameter(shader, pname)
    JSTypeHelper.toInt(ret)
  }

  final def getShaderParameterb(shader: Token.Shader, pname: Int): Boolean = {
    val ret = webGL.getShaderParameter(shader, pname)
    JSTypeHelper.toBoolean(ret)
  }

  final def getShaderPrecisionFormat(shadertype: Int, precisiontype: Int): PrecisionFormat = {
    val jsPrecisionFormat = webGL.getShaderPrecisionFormat(shadertype, precisiontype)
    PrecisionFormat(jsPrecisionFormat.rangeMin.toInt, jsPrecisionFormat.rangeMax.toInt, jsPrecisionFormat.precision.toInt)
  }

  final def getShaderInfoLog(shader: Token.Shader): String = {
    webGL.getShaderInfoLog(shader)
  }

  final def getShaderSource(shader: Token.Shader): String = {
    webGL.getShaderSource(shader)
  }

  final def getTexParameteri(target: Int, pname: Int): Int = {
    // org.scalajs.dom could maybe use return type Double instead of js.Any
    val ret = webGL.getTexParameter(target, pname)
    JSTypeHelper.toInt(ret)
  }

  final def getUniformi(program: Token.Program, location: Token.UniformLocation): Int = {
    val ret = webGL.getUniform(program, location)
    JSTypeHelper.toInt(ret)
  }

  final def getUniformiv(program: Token.Program, location: Token.UniformLocation, outputs: IntBuffer): Unit = {
    val ret = webGL.getUniform(program, location)
    JSTypeHelper.toInts(ret, outputs)
  }

  final def getUniformf(program: Token.Program, location: Token.UniformLocation): Float = {
    val ret = webGL.getUniform(program, location)
    JSTypeHelper.toFloat(ret)
  }

  final def getUniformfv(program: Token.Program, location: Token.UniformLocation, outputs: FloatBuffer): Unit = {
    val ret = webGL.getUniform(program, location)
    JSTypeHelper.toFloats(ret, outputs)
  }

  final def getUniformLocation(program: Token.Program, name: String): Token.UniformLocation = {
    webGL.getUniformLocation(program, name).asInstanceOf[Token.UniformLocation]
  }

  final def getVertexAttribi(index: Int, pname: Int): Int = {
    val ret = webGL.getVertexAttrib(index, pname)
    JSTypeHelper.toInt(ret)
  }

  final def getVertexAttribf(index: Int, pname: Int): Float = {
    val ret = webGL.getVertexAttrib(index, pname)
    JSTypeHelper.toFloat(ret)
  }

  final def getVertexAttribfv(index: Int, pname: Int, outputs: FloatBuffer): Unit = {
    val ret = webGL.getVertexAttrib(index, pname)
    JSTypeHelper.toFloats(ret, outputs)
  }

  final def getVertexAttribb(index: Int, pname: Int): Boolean = {
    val ret = webGL.getVertexAttrib(index, pname)
    JSTypeHelper.toBoolean(ret)
  }

  final def hint(target: Int, mode: Int): Unit = {
    // org.scalajs.dom has the return type wrong (js.Any instead of void), correct this once it's ok
    webGL.asInstanceOf[js.Dynamic].hint(target, mode)
  }

  final def isBuffer(buffer: Token.Buffer): Boolean = {
    if (buffer == null) false
    else webGL.isBuffer(buffer)
  }

  final def isEnabled(cap: Int): Boolean = {
    webGL.isEnabled(cap)
  }

  final def isFramebuffer(framebuffer: Token.FrameBuffer): Boolean = {
    if (framebuffer == null) false
    else webGL.isFramebuffer(framebuffer)
  }

  final def isProgram(program: Token.Program): Boolean = {
    if (program == null) false
    else webGL.isProgram(program)
  }

  final def isRenderbuffer(renderbuffer: Token.RenderBuffer): Boolean = {
    if (renderbuffer == null) false
    else webGL.isRenderbuffer(renderbuffer)
  }

  final def isShader(shader: Token.Shader): Boolean = {
    if (shader == null) false
    else webGL.isShader(shader)
  }

  final def isTexture(texture: Token.Texture): Boolean = {
    if (texture == null) false
    else webGL.isTexture(texture)
  }

  final def lineWidth(width: Float): Unit = {
    webGL.asInstanceOf[js.Dynamic].lineWidth(width)
    //gl.lineWidth(width)
  }

  final def linkProgram(program: Token.Program): Unit = {
    webGL.linkProgram(program)
  }

  final def pixelStorei(pname: Int, param: Int): Unit = {
    webGL.pixelStorei(pname, param)
  }

  final def polygonOffset(factor: Float, units: Float): Unit = {
    webGL.asInstanceOf[js.Dynamic].polygonOffset(factor, units)
    //gl.polygonOffset(factor, units)
  }

  private final def _readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: Buffer): Unit = {
    val buffer: Buffer = pixels
    if (pixels != null) require(buffer.hasArrayBuffer())
    webGL.readPixels(x, y, width, height, format, `type`, if (pixels != null) buffer.dataView() else null)
  }

  final def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: ByteBuffer): Unit =
    this._readPixels(x, y, width, height, format, `type`, pixels.slice)
  final def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: ShortBuffer): Unit =
    this._readPixels(x, y, width, height, format, `type`, pixels.slice)
  final def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: IntBuffer): Unit =
    this._readPixels(x, y, width, height, format, `type`, pixels.slice)
  final def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: FloatBuffer): Unit =
    this._readPixels(x, y, width, height, format, `type`, pixels.slice)
  final def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: DoubleBuffer): Unit =
    this._readPixels(x, y, width, height, format, `type`, pixels.slice)

  final def renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int): Unit = {
    webGL.renderbufferStorage(target, internalformat, width, height)
  }

  final def sampleCoverage(value: Float, invert: Boolean): Unit = {
    webGL.asInstanceOf[js.Dynamic].sampleCoverage(value, invert)
    //gl.sampleCoverage(value, invert)
  }

  final def scissor(x: Int, y: Int, width: Int, height: Int): Unit = {
    webGL.scissor(x, y, width, height)
  }

  final def shaderSource(shader: Token.Shader, source: String): Unit = {
    webGL.shaderSource(shader, source)
  }

  final def stencilFunc(func: Int, ref: Int, mask: Int): Unit = {
    webGL.stencilFunc(func, ref, mask)
  }

  final def stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int): Unit = {
    webGL.stencilFuncSeparate(face, func, ref, mask)
  }

  final def stencilMask(mask: Int): Unit = {
    webGL.stencilMask(mask)
  }

  final def stencilMaskSeparate(face: Int, mask: Int): Unit = {
    // TODO
    //gl.stencilMaskSeperate(face, mask)
    webGL.asInstanceOf[js.Dynamic].stencilMaskSeparate(face, mask)
  }

  final def stencilOp(fail: Int, zfail: Int, zpass: Int): Unit = {
    webGL.stencilOp(fail, zfail, zpass)
  }

  final def stencilOpSeparate(face: Int, sfail: Int, dpfail: Int, dppass: Int): Unit = {
    // TODO
    //gl.stencilOpSeperate(face, sfail, dpfail, dppass)
    webGL.asInstanceOf[js.Dynamic].stencilOpSeparate(face, sfail, dpfail, dppass)
  }

  private final def _texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                                format: Int, `type`: Int, pixels: Buffer): Unit = {

    val buffer: Buffer = pixels
    if (pixels != null) require(buffer.hasArrayBuffer())
    webGL.texImage2D(target, level, internalformat, width, height, border, format, `type`, if (pixels != null) buffer.dataView() else null)
  }

  final def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                       format: Int, `type`: Int, pixels: ByteBuffer): Unit = this._texImage2D(target, level, internalformat, width, height, border,
    format, `type`, if (pixels != null) pixels.slice else null)
  final def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                       format: Int, `type`: Int, pixels: ShortBuffer): Unit = this._texImage2D(target, level, internalformat, width, height, border,
    format, `type`, if (pixels != null) pixels.slice else null)
  final def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                       format: Int, `type`: Int, pixels: IntBuffer): Unit = this._texImage2D(target, level, internalformat, width, height, border,
    format, `type`, if (pixels != null) pixels.slice else null)
  final def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                       format: Int, `type`: Int, pixels: FloatBuffer): Unit = this._texImage2D(target, level, internalformat, width, height, border,
    format, `type`, if (pixels != null) pixels.slice else null)
  final def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                       format: Int, `type`: Int, pixels: DoubleBuffer): Unit = this._texImage2D(target, level, internalformat, width, height, border,
    format, `type`, if (pixels != null) pixels.slice else null)
  final def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                       format: Int, `type`: Int): Unit = {
    webGL.texImage2D(target, level, internalformat, width, height, border, format, `type`, null)
  }

  final def texParameterf(target: Int, pname: Int, param: Float): Unit = {
    webGL.asInstanceOf[js.Dynamic].texParameterf(target, pname, param)
    //gl.texParameterf(target, pname, param)
  }

  final def texParameteri(target: Int, pname: Int, param: Int): Unit = {
    webGL.texParameteri(target, pname, param)
  }

  private final def _texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                                   format: Int, `type`: Int, pixels: Buffer): Unit = {

    val buffer: Buffer = pixels
    if (pixels != null) require(buffer.hasArrayBuffer())
    webGL.texSubImage2D(target, level, xoffset, yoffset, width, height, format, `type`, if (pixels != null) buffer.dataView() else null)
  }

  final def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                          format: Int, `type`: Int, pixels: ByteBuffer): Unit = this._texSubImage2D(target, level, xoffset, yoffset, width, height,
    format, `type`, pixels.slice)
  final def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                          format: Int, `type`: Int, pixels: ShortBuffer): Unit = this._texSubImage2D(target, level, xoffset, yoffset, width, height,
    format, `type`, pixels.slice)
  final def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                          format: Int, `type`: Int, pixels: IntBuffer): Unit = this._texSubImage2D(target, level, xoffset, yoffset, width, height,
    format, `type`, pixels.slice)
  final def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                          format: Int, `type`: Int, pixels: FloatBuffer): Unit = this._texSubImage2D(target, level, xoffset, yoffset, width, height,
    format, `type`, pixels.slice)
  final def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                          format: Int, `type`: Int, pixels: DoubleBuffer): Unit = this._texSubImage2D(target, level, xoffset, yoffset, width, height,
    format, `type`, pixels.slice)

  final def uniform1f(location: Token.UniformLocation, x: Float): Unit = {
    webGL.uniform1f(location, x)
  }

  final def uniform1fv(location: Token.UniformLocation, values: FloatBuffer): Unit = {
    val slice = values.slice
    require(slice.hasTypedArray())
    webGL.uniform1fv(location, slice.typedArray())
  }

  final def uniform1i(location: Token.UniformLocation, x: Int): Unit = {
    webGL.uniform1i(location, x)
  }

  final def uniform1iv(location: Token.UniformLocation, values: IntBuffer): Unit = {
    val slice = values.slice
    require(slice.hasTypedArray())
    webGL.uniform1iv(location, slice.typedArray())
  }

  final def uniform2f(location: Token.UniformLocation, x: Float, y: Float): Unit = {
    webGL.uniform2f(location, x, y)
  }

  final def uniform2fv(location: Token.UniformLocation, values: FloatBuffer): Unit = {
    val slice = values.slice
    require(slice.hasTypedArray())
    webGL.uniform2fv(location, slice.typedArray())
  }

  final def uniform2i(location: Token.UniformLocation, x: Int, y: Int): Unit = {
    webGL.uniform2i(location, x, y)
  }

  final def uniform2iv(location: Token.UniformLocation, values: IntBuffer): Unit = {
    val slice = values.slice
    require(slice.hasTypedArray())
    webGL.uniform2iv(location, slice.typedArray())
  }

  final def uniform3f(location: Token.UniformLocation, x: Float, y: Float, z: Float): Unit = {
    webGL.uniform3f(location, x, y, z)
  }

  final def uniform3fv(location: Token.UniformLocation, values: FloatBuffer): Unit = {
    val slice = values.slice
    require(slice.hasTypedArray())
    webGL.uniform3fv(location, slice.typedArray())
  }

  final def uniform3i(location: Token.UniformLocation, x: Int, y: Int, z: Int): Unit = {
    webGL.uniform3i(location, x, y, z)
  }

  final def uniform3iv(location: Token.UniformLocation, values: IntBuffer): Unit = {
    val slice = values.slice
    require(slice.hasTypedArray())
    webGL.uniform3iv(location, slice.typedArray())
  }

  final def uniform4f(location: Token.UniformLocation, x: Float, y: Float, z: Float, w: Float): Unit = {
    webGL.uniform4f(location, x, y, z, w)
  }

  final def uniform4fv(location: Token.UniformLocation, values: FloatBuffer): Unit = {
    val slice = values.slice
    require(slice.hasTypedArray())
    webGL.uniform4fv(location, slice.typedArray())
  }

  final def uniform4i(location: Token.UniformLocation, x: Int, y: Int, z: Int, w: Int): Unit = {
    webGL.uniform4i(location, x, y, z, w)
  }

  final def uniform4iv(location: Token.UniformLocation, values: IntBuffer): Unit = {
    val slice = values.slice
    require(slice.hasTypedArray())
    webGL.uniform4iv(location, slice.typedArray())
  }

  final def uniformMatrix2fv(location: Token.UniformLocation, transpose: Boolean, matrices: FloatBuffer): Unit = {
    val slice = matrices.slice
    require(slice.hasTypedArray())
    webGL.uniformMatrix2fv(location, transpose, slice.typedArray())
  }

  final def uniformMatrix3fv(location: Token.UniformLocation, transpose: Boolean, matrices: FloatBuffer): Unit = {
    val slice = matrices.slice
    require(slice.hasTypedArray())
    webGL.uniformMatrix3fv(location, transpose, slice.typedArray())
  }

  final def uniformMatrix4fv(location: Token.UniformLocation, transpose: Boolean, matrices: FloatBuffer): Unit = {
    val slice = matrices.slice
    require(slice.hasTypedArray())
    webGL.uniformMatrix4fv(location, transpose, slice.typedArray())
  }

  final def useProgram(program: Token.Program): Unit = {
    webGL.useProgram(program)
  }

  final def validateProgram(program: Token.Program): Unit = {
    webGL.validateProgram(program)
  }

  final def vertexAttrib1f(index: Int, x: Float): Unit = {
    webGL.vertexAttrib1f(index, x)
  }

  final def vertexAttrib1fv(index: Int, values: FloatBuffer): Unit = {
    val slice = values.slice
    require(slice.hasTypedArray())
    webGL.vertexAttrib1fv(index, slice.typedArray())
  }

  final def vertexAttrib2f(index: Int, x: Float, y: Float): Unit = {
    webGL.vertexAttrib2f(index, x, y)
  }

  final def vertexAttrib2fv(index: Int, values: FloatBuffer): Unit = {
    val slice = values.slice
    require(slice.hasTypedArray())
    webGL.vertexAttrib2fv(index, slice.typedArray())
  }

  final def vertexAttrib3f(index: Int, x: Float, y: Float, z: Float): Unit = {
    webGL.vertexAttrib3f(index, x, y, z)
  }

  final def vertexAttrib3fv(index: Int, values: FloatBuffer): Unit = {
    val slice = values.slice
    require(slice.hasTypedArray())
    webGL.vertexAttrib3fv(index, slice.typedArray())
  }

  final def vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float): Unit = {
    webGL.vertexAttrib4f(index, x, y, z, w)
  }

  final def vertexAttrib4fv(index: Int, values: FloatBuffer): Unit = {
    val slice = values.slice
    require(slice.hasTypedArray())
    webGL.vertexAttrib4fv(index, slice.typedArray())
  }

  final def vertexAttribPointer(index: Int, size: Int, `type`: Int, normalized: Boolean, stride: Int, offset: Long): Unit = {
    webGL.vertexAttribPointer(index, size, `type`, normalized, stride, offset.toInt)
  }

  final def viewport(x: Int, y: Int, width: Int, height: Int): Unit = {
    webGL.viewport(x, y, width, height)
  }

  // Helper methods

  final def errorMessage(code: Int): String = {
    //val msg: String = g.WebGLDebugUtils.glEnumToString(code).asInstanceOf[String] // Not available
    val msg: String = "Error code " + code
    msg
  }

  final def validProgram(program: Token.Program): Boolean = {
    (program != null) && this.isProgram(program)
  }

  final def validShader(shader: Token.Shader): Boolean = {
    (shader != null) && this.isShader(shader)
  }

  final def validBuffer(buffer: Token.Buffer): Boolean = {
    (buffer != null) && this.isBuffer(buffer)
  }

  final def validUniformLocation(uloc: Token.UniformLocation): Boolean = {
    (uloc != null)
  }

  final def validFramebuffer(fb: Token.FrameBuffer): Boolean = {
    (fb != null) && this.isFramebuffer(fb)
  }

  final def validRenderbuffer(rb: Token.RenderBuffer): Boolean = {
    (rb != null) && this.isRenderbuffer(rb)
  }

  final def differentPrograms(p1: Token.Program, p2: Token.Program): Boolean = {
    p1 != p2
  }
}

private[games] object JSTypeHelper {
  def toBoolean(value: js.Any): Boolean = {
    val typeName = JsUtils.typeName(value)
    typeName match {
      case "Boolean" => value.asInstanceOf[Boolean]
      case "Number"  => this.jsNumberToBoolean(value.asInstanceOf[Double])
      case "Array" =>
        val jsArray = value.asInstanceOf[js.Array[js.Any]]
        toBoolean(jsArray(0))

      case "Int8Array"    => this.jsNumberToBoolean(value.asInstanceOf[js.typedarray.Int8Array](0))
      case "Uint8Array"   => this.jsNumberToBoolean(value.asInstanceOf[js.typedarray.Uint8Array](0))
      case "Int16Array"   => this.jsNumberToBoolean(value.asInstanceOf[js.typedarray.Int16Array](0))
      case "Uint16Array"  => this.jsNumberToBoolean(value.asInstanceOf[js.typedarray.Uint16Array](0))
      case "Int32Array"   => this.jsNumberToBoolean(value.asInstanceOf[js.typedarray.Int32Array](0))
      case "Uint32Array"  => this.jsNumberToBoolean(value.asInstanceOf[js.typedarray.Uint32Array](0))
      case "Float32Array" => this.jsNumberToBoolean(value.asInstanceOf[js.typedarray.Float32Array](0))
      case "Float64Array" => this.jsNumberToBoolean(value.asInstanceOf[js.typedarray.Float64Array](0))
      case _              => throw new RuntimeException("Cannot convert type " + typeName + " to boolean")
    }
  }

  def toInt(value: js.Any): Int = {
    val typeName = JsUtils.typeName(value)
    typeName match {
      case "Boolean" => this.booleanToInt(value.asInstanceOf[Boolean])
      case "Number"  => value.asInstanceOf[Double].toInt
      case "Array" =>
        val jsArray = value.asInstanceOf[js.Array[js.Any]]
        toInt(jsArray(0))

      case "Int8Array"    => value.asInstanceOf[js.typedarray.Int8Array](0).toInt
      case "Uint8Array"   => value.asInstanceOf[js.typedarray.Uint8Array](0).toInt
      case "Int16Array"   => value.asInstanceOf[js.typedarray.Int16Array](0).toInt
      case "Uint16Array"  => value.asInstanceOf[js.typedarray.Uint16Array](0).toInt
      case "Int32Array"   => value.asInstanceOf[js.typedarray.Int32Array](0).toInt
      case "Uint32Array"  => value.asInstanceOf[js.typedarray.Uint32Array](0).toInt
      case "Float32Array" => this.normalizedFloatToSignedInt(value.asInstanceOf[js.typedarray.Float32Array](0).toDouble)
      case "Float64Array" => this.normalizedFloatToSignedInt(value.asInstanceOf[js.typedarray.Float64Array](0).toDouble)
      case _              => throw new RuntimeException("Cannot convert type " + typeName + " to int")
    }
  }

  def toShort(value: js.Any): Short = {
    val typeName = JsUtils.typeName(value)
    typeName match {
      case "Boolean" => this.booleanToShort(value.asInstanceOf[Boolean])
      case "Number"  => value.asInstanceOf[Double].toShort
      case "Array" =>
        val jsArray = value.asInstanceOf[js.Array[js.Any]]
        toShort(jsArray(0))

      case "Int8Array"    => value.asInstanceOf[js.typedarray.Int8Array](0).toShort
      case "Uint8Array"   => value.asInstanceOf[js.typedarray.Uint8Array](0).toShort
      case "Int16Array"   => value.asInstanceOf[js.typedarray.Int16Array](0).toShort
      case "Uint16Array"  => value.asInstanceOf[js.typedarray.Uint16Array](0).toShort
      case "Int32Array"   => value.asInstanceOf[js.typedarray.Int32Array](0).toShort
      case "Uint32Array"  => value.asInstanceOf[js.typedarray.Uint32Array](0).toShort
      case "Float32Array" => this.normalizedFloatToSignedShort(value.asInstanceOf[js.typedarray.Float32Array](0).toDouble)
      case "Float64Array" => this.normalizedFloatToSignedShort(value.asInstanceOf[js.typedarray.Float64Array](0).toDouble)
      case _              => throw new RuntimeException("Cannot convert type " + typeName + " to short")
    }
  }

  def toFloat(value: js.Any): Float = {
    val typeName = JsUtils.typeName(value)
    typeName match {
      case "Boolean" => this.booleanToFloat(value.asInstanceOf[Boolean])
      case "Number"  => value.asInstanceOf[Double].toFloat
      case "Array" =>
        val jsArray = value.asInstanceOf[js.Array[js.Any]]
        toFloat(jsArray(0))

      case "Int8Array"    => this.signedByteToNormalizedFloat(value.asInstanceOf[js.typedarray.Int8Array](0).toByte).toFloat
      case "Uint8Array"   => this.unsignedByteToNormalizedFloat(value.asInstanceOf[js.typedarray.Uint8Array](0).toByte).toFloat
      case "Int16Array"   => this.signedShortToNormalizedFloat(value.asInstanceOf[js.typedarray.Int16Array](0).toShort).toFloat
      case "Uint16Array"  => this.unsignedShortToNormalizedFloat(value.asInstanceOf[js.typedarray.Uint16Array](0).toShort).toFloat
      case "Int32Array"   => this.signedIntToNormalizedFloat(value.asInstanceOf[js.typedarray.Int32Array](0).toInt).toFloat
      case "Uint32Array"  => this.unsignedIntToNormalizedFloat(value.asInstanceOf[js.typedarray.Uint32Array](0).toInt).toFloat
      case "Float32Array" => value.asInstanceOf[js.typedarray.Float32Array](0).toFloat
      case "Float64Array" => value.asInstanceOf[js.typedarray.Float64Array](0).toFloat
      case _              => throw new RuntimeException("Cannot convert type " + typeName + " to float")
    }
  }

  def toDouble(value: js.Any): Double = {
    val typeName = JsUtils.typeName(value)
    typeName match {
      case "Boolean" => this.booleanToFloat(value.asInstanceOf[Boolean])
      case "Number"  => value.asInstanceOf[Double].toDouble
      case "Array" =>
        val jsArray = value.asInstanceOf[js.Array[js.Any]]
        toDouble(jsArray(0))

      case "Int8Array"    => this.signedByteToNormalizedFloat(value.asInstanceOf[js.typedarray.Int8Array](0).toByte).toDouble
      case "Uint8Array"   => this.unsignedByteToNormalizedFloat(value.asInstanceOf[js.typedarray.Uint8Array](0).toByte).toDouble
      case "Int16Array"   => this.signedShortToNormalizedFloat(value.asInstanceOf[js.typedarray.Int16Array](0).toShort).toDouble
      case "Uint16Array"  => this.unsignedShortToNormalizedFloat(value.asInstanceOf[js.typedarray.Uint16Array](0).toShort).toDouble
      case "Int32Array"   => this.signedIntToNormalizedFloat(value.asInstanceOf[js.typedarray.Int32Array](0).toInt).toDouble
      case "Uint32Array"  => this.unsignedIntToNormalizedFloat(value.asInstanceOf[js.typedarray.Uint32Array](0).toInt).toDouble
      case "Float32Array" => value.asInstanceOf[js.typedarray.Float32Array](0).toDouble
      case "Float64Array" => value.asInstanceOf[js.typedarray.Float64Array](0).toDouble
      case _              => throw new RuntimeException("Cannot convert type " + typeName + " to double")
    }
  }

  def toBooleans(value: js.Any, data: ByteBuffer): Unit = {
    val slice = data.slice

    val typeName = JsUtils.typeName(value)
    typeName match {
      case "Boolean" => slice.put(this.booleanToByte(value.asInstanceOf[Boolean]))
      case "Number"  => slice.put(this.booleanToByte(this.jsNumberToBoolean(value.asInstanceOf[Double])))
      case "Array" =>
        val jsArray = value.asInstanceOf[js.Array[js.Any]]
        val length = jsArray.length.toInt
        val containedType = JsUtils.typeName(jsArray(0))

        require(slice.remaining >= length)

        containedType match {
          case "Boolean" => jsArray.foreach { e => slice.put(this.booleanToByte(e.asInstanceOf[Boolean])) }
          case "Number"  => jsArray.foreach { e => slice.put(this.booleanToByte(this.jsNumberToBoolean(e.asInstanceOf[Double]))) }
          case _         => throw new RuntimeException("Cannot convert array of " + containedType + " to booleans")
        }

      case "Int8Array" =>
        val length = value.asInstanceOf[js.Dynamic].length.asInstanceOf[Double].toInt
        val array = value.asInstanceOf[js.typedarray.Int8Array]
        val l = array
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          data.put(this.booleanToByte(this.jsNumberToBoolean(array(i).asInstanceOf[Double])))
        }

      case "Uint8Array" =>
        val length = value.asInstanceOf[js.Dynamic].length.asInstanceOf[Double].toInt
        val array = value.asInstanceOf[js.typedarray.Uint8Array]
        val l = array
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          data.put(this.booleanToByte(this.jsNumberToBoolean(array(i).asInstanceOf[Double])))
        }

      case "Int16Array" =>
        val length = value.asInstanceOf[js.Dynamic].length.asInstanceOf[Double].toInt
        val array = value.asInstanceOf[js.typedarray.Int16Array]
        val l = array
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          data.put(this.booleanToByte(this.jsNumberToBoolean(array(i).asInstanceOf[Double])))
        }

      case "Uint16Array" =>
        val length = value.asInstanceOf[js.Dynamic].length.asInstanceOf[Double].toInt
        val array = value.asInstanceOf[js.typedarray.Uint16Array]
        val l = array
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          data.put(this.booleanToByte(this.jsNumberToBoolean(array(i).asInstanceOf[Double])))
        }

      case "Int32Array" =>
        val length = value.asInstanceOf[js.Dynamic].length.asInstanceOf[Double].toInt
        val array = value.asInstanceOf[js.typedarray.Int32Array]
        val l = array
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          data.put(this.booleanToByte(this.jsNumberToBoolean(array(i).asInstanceOf[Double])))
        }

      case "Uint32Array" =>
        val length = value.asInstanceOf[js.Dynamic].length.asInstanceOf[Double].toInt
        val array = value.asInstanceOf[js.typedarray.Uint32Array]
        val l = array
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          data.put(this.booleanToByte(this.jsNumberToBoolean(array(i).asInstanceOf[Double])))
        }

      case "Float32Array" =>
        val length = value.asInstanceOf[js.Dynamic].length.asInstanceOf[Double].toInt
        val array = value.asInstanceOf[js.typedarray.Float32Array]
        val l = array
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          data.put(this.booleanToByte(this.jsNumberToBoolean(array(i).asInstanceOf[Double])))
        }

      case "Float64Array" =>
        val length = value.asInstanceOf[js.Dynamic].length.asInstanceOf[Double].toInt
        val array = value.asInstanceOf[js.typedarray.Float64Array]
        val l = array
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          data.put(this.booleanToByte(this.jsNumberToBoolean(array(i).asInstanceOf[Double])))
        }

      case _ => throw new RuntimeException("Cannot convert type " + typeName + " to booleans")
    }
  }

  def toInts(value: js.Any, data: IntBuffer): Unit = {
    val slice = data.slice

    val typeName = JsUtils.typeName(value)
    typeName match {
      case "Boolean" => slice.put(this.booleanToByte(value.asInstanceOf[Boolean]))
      case "Number"  => slice.put(value.asInstanceOf[Double].toInt)
      case "Array" =>
        val jsArray = value.asInstanceOf[js.Array[js.Any]]
        val length = jsArray.length.toInt
        val containedType = JsUtils.typeName(jsArray(0))

        require(slice.remaining >= length)

        containedType match {
          case "Boolean" => jsArray.foreach { e => slice.put(this.booleanToByte(e.asInstanceOf[Boolean])) }
          case "Number"  => jsArray.foreach { e => slice.put(e.asInstanceOf[Double].toInt) }
          case _         => throw new RuntimeException("Cannot convert array of " + containedType + " to ints")
        }

      case "Int8Array" =>
        val array = value.asInstanceOf[js.typedarray.Int8Array]
        val length = value.asInstanceOf[js.Dynamic].length.asInstanceOf[Double].toInt
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          slice.put(array(i).toInt)
        }

      case "Uint8Array" =>
        val array = value.asInstanceOf[js.typedarray.Uint8Array]
        val length = value.asInstanceOf[js.Dynamic].length.asInstanceOf[Double].toInt
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          slice.put(array(i).toInt)
        }

      case "Int16Array" =>
        val array = value.asInstanceOf[js.typedarray.Int16Array]
        val length = value.asInstanceOf[js.Dynamic].length.asInstanceOf[Double].toInt
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          slice.put(array(i).toInt)
        }

      case "Uint16Array" =>
        val array = value.asInstanceOf[js.typedarray.Uint16Array]
        val length = value.asInstanceOf[js.Dynamic].length.asInstanceOf[Double].toInt
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          slice.put(array(i).toInt)
        }

      case "Int32Array" =>
        val array = value.asInstanceOf[js.typedarray.Int32Array]
        val length = value.asInstanceOf[js.Dynamic].length.asInstanceOf[Double].toInt
        require(slice.remaining >= length)

        if (slice.hasTypedArray()) { // optimized version for native buffer
          slice.typedArray().set(array)
        } else { // generic version
          for (i <- 0 until length) {
            slice.put(array(i).toInt)
          }
        }

      case "Uint32Array" =>
        val array = value.asInstanceOf[js.typedarray.Uint32Array]
        val length = value.asInstanceOf[js.Dynamic].length.asInstanceOf[Double].toInt
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          slice.put(array(i).toInt)
        }

      case "Float32Array" =>
        val array = value.asInstanceOf[js.typedarray.Float32Array]
        val length = value.asInstanceOf[js.Dynamic].length.asInstanceOf[Double].toInt
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          slice.put(this.normalizedFloatToSignedInt(array(i).toFloat))
        }

      case "Float64Array" =>
        val array = value.asInstanceOf[js.typedarray.Float64Array]
        val length = value.asInstanceOf[js.Dynamic].length.asInstanceOf[Double].toInt
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          slice.put(this.normalizedFloatToSignedInt(array(i).toDouble))
        }

      case _ => throw new RuntimeException("Cannot convert type " + typeName + " to ints")
    }
  }

  def toFloats(value: js.Any, data: FloatBuffer): Unit = {
    val slice = data.slice

    val typeName = JsUtils.typeName(value)
    typeName match {
      case "Boolean" => slice.put(this.booleanToByte(value.asInstanceOf[Boolean]))
      case "Number"  => slice.put(value.asInstanceOf[Double].toFloat)
      case "Array" =>
        val jsArray = value.asInstanceOf[js.Array[js.Any]]
        val length = jsArray.length.toInt
        val containedType = JsUtils.typeName(jsArray(0))

        require(slice.remaining >= length)

        containedType match {
          case "Boolean" => jsArray.foreach { e => slice.put(this.booleanToByte(e.asInstanceOf[Boolean])) }
          case "Number"  => jsArray.foreach { e => slice.put(e.asInstanceOf[Double].toFloat) }
          case _         => throw new RuntimeException("Cannot convert array of " + containedType + " to floats")
        }

      case "Int8Array" =>
        val array = value.asInstanceOf[js.typedarray.Int8Array]
        val length = array.length
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          slice.put(this.signedByteToNormalizedFloat(array(i).toByte).toFloat)
        }

      case "Uint8Array" =>
        val array = value.asInstanceOf[js.typedarray.Uint8Array]
        val length = array.length
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          slice.put(this.unsignedByteToNormalizedFloat(array(i).toByte).toFloat)
        }

      case "Int16Array" =>
        val array = value.asInstanceOf[js.typedarray.Int16Array]
        val length = array.length
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          slice.put(this.signedShortToNormalizedFloat(array(i).toShort).toFloat)
        }

      case "Uint16Array" =>
        val array = value.asInstanceOf[js.typedarray.Uint16Array]
        val length = array.length
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          slice.put(this.unsignedShortToNormalizedFloat(array(i).toShort).toFloat)
        }

      case "Int32Array" =>
        val array = value.asInstanceOf[js.typedarray.Int32Array]
        val length = array.length
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          slice.put(this.signedIntToNormalizedFloat(array(i).toInt).toFloat)
        }

      case "Uint32Array" =>
        val array = value.asInstanceOf[js.typedarray.Uint32Array]
        val length = array.length
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          slice.put(this.unsignedIntToNormalizedFloat(array(i).toInt).toFloat)
        }

      case "Float32Array" =>
        val array = value.asInstanceOf[js.typedarray.Float32Array]
        val length = array.length
        require(slice.remaining >= length)

        if (slice.hasTypedArray()) { // optimized version for native buffer
          slice.typedArray().set(array)
        } else { // generic version
          for (i <- 0 until length) {
            slice.put(array(i).toFloat)
          }
        }

      case "Float64Array" =>
        val array = value.asInstanceOf[js.typedarray.Float64Array]
        val length = array.length
        require(slice.remaining >= length)

        for (i <- 0 until length) {
          slice.put(array(i).toFloat)
        }

      case _ => throw new RuntimeException("Cannot convert type " + typeName + " to floats")
    }
  }

  // Auxiliary methods

  def jsNumberToBoolean(v: Double): Boolean = {
    val zero: Double = 0
    v != zero
  }

  def booleanToJsNumber(b: Boolean): Double = {
    if (b) 1.0
    else 0.0
  }

  def byteToBoolean(b: Byte): Boolean = {
    b != 0
  }

  def booleanToByte(b: Boolean): Byte = {
    if (b) 1
    else 0
  }

  def shortToBoolean(s: Short): Boolean = {
    s != 0
  }

  def booleanToShort(b: Boolean): Short = {
    if (b) 1
    else 0
  }

  def intToBoolean(i: Int): Boolean = {
    i != 0
  }

  def booleanToInt(b: Boolean): Int = {
    if (b) 1
    else 0
  }

  def floatToBoolean(f: Float): Boolean = {
    f != 0.0f
  }

  def booleanToFloat(b: Boolean): Float = {
    if (b) 1.0f
    else 0.0f
  }

  // Conversions algorithm taken from the official OpenGL ES 2.0 specifications (2.1.2)

  val maxUint32: Long = 0xFFFFFFFFL
  val maxUint32d: Double = maxUint32.toDouble // seems faster than Long for math operations in JavaScript

  val maxUint16: Int = 0xFFFF
  val maxUint16d: Double = maxUint16.toDouble

  val maxUint8: Int = 0xFF
  val maxUint8d: Double = maxUint8.toDouble

  // From/To 32 bits values

  /*
   * Convert an unsigned int to a normalized floating-point value
   * [0, maxUint32] -> [0, 1]
   */
  def unsignedIntToNormalizedFloat(c: Int): Double = {
    val cl = (c.toLong & maxUint32) // unsign the value
    cl.toDouble / maxUint32d
  }

  /*
   * Convert a signed int to a normalized floating-point value
   * [minInt32, maxInt32] -> [-1, 1]
   */
  def signedIntToNormalizedFloat(c: Int): Double = {
    (c.toDouble * 2 + 1) / maxUint32d
  }

  /*
   * Convert a normalized floating-point value to an unsigned int
   * [0, 1] -> [0, maxUint32]
   */
  def normalizedFloatToUnsignedInt(f: Double): Int = {
    val fb = if (f > 1.0) 1.0 else if (f < 0.0) 0.0 else f // clamp
    (fb * maxUint32d).toInt
  }

  /*
   * Convert a normalized floating-point value to a signed int
   * [-1, 1] -> [minInt32, maxInt32]
   */
  def normalizedFloatToSignedInt(f: Double): Int = {
    val fb = if (f > 1.0) 1.0 else if (f < -1.0) -1.0 else f // clamp
    ((fb * maxUint32d - 1) / 2).toInt
  }

  // From/To 16 bits values

  /*
   * Convert an unsigned short to a normalized floating-point value
   * [0, maxUint16] -> [0, 1]
   */
  def unsignedShortToNormalizedFloat(c: Short): Double = {
    val cl = (c.toInt & maxUint16) // unsign the value
    cl.toDouble / maxUint16d
  }

  /*
   * Convert a signed short to a normalized floating-point value
   * [minInt16, maxInt16] -> [-1, 1]
   */
  def signedShortToNormalizedFloat(c: Short): Double = {
    (c.toDouble * 2 + 1) / maxUint16d
  }

  /*
   * Convert a normalized floating-point value to an unsigned short
   * [0, 1] -> [0, maxUint16]
   */
  def normalizedFloatToUnsignedShort(f: Double): Short = {
    val fb = if (f > 1.0) 1.0 else if (f < 0.0) 0.0 else f // clamp
    (fb * maxUint16d).toShort
  }

  /*
   * Convert a normalized floating-point value to a signed short
   * [-1, 1] -> [minInt16, maxInt16]
   */
  def normalizedFloatToSignedShort(f: Double): Short = {
    val fb = if (f > 1.0) 1.0 else if (f < -1.0) -1.0 else f // clamp
    ((fb * maxUint16d - 1) / 2).toShort
  }

  // From/To 8 bits values

  /*
   * Convert an unsigned byte to a normalized floating-point value
   * [0, maxUint8] -> [0, 1]
   */
  def unsignedByteToNormalizedFloat(c: Byte): Double = {
    val cl = (c.toShort & maxUint8) // unsign the value
    c.toDouble / maxUint8d
  }

  /*
   * Convert a signed byte to a normalized floating-point value
   * [minInt8, maxInt8] -> [-1, 1]
   */
  def signedByteToNormalizedFloat(c: Byte): Double = {
    (c.toDouble * 2 + 1) / maxUint8d
  }

  /*
   * Convert a normalized floating-point value to an unsigned byte
   * [0, 1] -> [0, maxUint8]
   */
  def normalizedFloatToUnsignedByte(f: Double): Byte = {
    val fb = if (f > 1.0) 1.0 else if (f < 0.0) 0.0 else f // clamp
    (fb * maxUint8d).toByte
  }

  /*
   * Convert a normalized floating-point value to a signed byte
   * [-1, 1] -> [minInt8, maxInt8]
   */
  def normalizedFloatToSignedByte(f: Double): Byte = {
    val fb = if (f > 1.0) 1.0 else if (f < -1.0) -1.0 else f // clamp
    ((fb * maxUint8d - 1) / 2).toByte
  }
}

trait GLES2CompImpl extends GLES2CompRequirements {

  /* public API - constants */

  final val FALSE: Int = 0
  final val TRUE: Int = 1

  final val DEPTH_BUFFER_BIT: Int = dom.raw.WebGLRenderingContext.DEPTH_BUFFER_BIT.toInt
  final val STENCIL_BUFFER_BIT: Int = dom.raw.WebGLRenderingContext.STENCIL_BUFFER_BIT.toInt
  final val COLOR_BUFFER_BIT: Int = dom.raw.WebGLRenderingContext.COLOR_BUFFER_BIT.toInt
  final val POINTS: Int = dom.raw.WebGLRenderingContext.POINTS.toInt
  final val LINES: Int = dom.raw.WebGLRenderingContext.LINES.toInt
  final val LINE_LOOP: Int = dom.raw.WebGLRenderingContext.LINE_LOOP.toInt
  final val LINE_STRIP: Int = dom.raw.WebGLRenderingContext.LINE_STRIP.toInt
  final val TRIANGLES: Int = dom.raw.WebGLRenderingContext.TRIANGLES.toInt
  final val TRIANGLE_STRIP: Int = dom.raw.WebGLRenderingContext.TRIANGLE_STRIP.toInt
  final val TRIANGLE_FAN: Int = dom.raw.WebGLRenderingContext.TRIANGLE_FAN.toInt
  final val ZERO: Int = dom.raw.WebGLRenderingContext.ZERO.toInt
  final val ONE: Int = dom.raw.WebGLRenderingContext.ONE.toInt
  final val SRC_COLOR: Int = dom.raw.WebGLRenderingContext.SRC_COLOR.toInt
  final val ONE_MINUS_SRC_COLOR: Int = dom.raw.WebGLRenderingContext.ONE_MINUS_SRC_COLOR.toInt
  final val SRC_ALPHA: Int = dom.raw.WebGLRenderingContext.SRC_ALPHA.toInt
  final val ONE_MINUS_SRC_ALPHA: Int = dom.raw.WebGLRenderingContext.ONE_MINUS_SRC_ALPHA.toInt
  final val DST_ALPHA: Int = dom.raw.WebGLRenderingContext.DST_ALPHA.toInt
  final val ONE_MINUS_DST_ALPHA: Int = dom.raw.WebGLRenderingContext.ONE_MINUS_DST_ALPHA.toInt
  final val DST_COLOR: Int = dom.raw.WebGLRenderingContext.DST_COLOR.toInt
  final val ONE_MINUS_DST_COLOR: Int = dom.raw.WebGLRenderingContext.ONE_MINUS_DST_COLOR.toInt
  final val SRC_ALPHA_SATURATE: Int = dom.raw.WebGLRenderingContext.SRC_ALPHA_SATURATE.toInt
  final val FUNC_ADD: Int = dom.raw.WebGLRenderingContext.FUNC_ADD.toInt
  final val BLEND_EQUATION: Int = dom.raw.WebGLRenderingContext.BLEND_EQUATION.toInt
  final val BLEND_EQUATION_RGB: Int = dom.raw.WebGLRenderingContext.BLEND_EQUATION_RGB.toInt
  final val BLEND_EQUATION_ALPHA: Int = dom.raw.WebGLRenderingContext.BLEND_EQUATION_ALPHA.toInt
  final val FUNC_SUBTRACT: Int = dom.raw.WebGLRenderingContext.FUNC_SUBTRACT.toInt
  final val FUNC_REVERSE_SUBTRACT: Int = dom.raw.WebGLRenderingContext.FUNC_REVERSE_SUBTRACT.toInt
  final val BLEND_DST_RGB: Int = dom.raw.WebGLRenderingContext.BLEND_DST_RGB.toInt
  final val BLEND_SRC_RGB: Int = dom.raw.WebGLRenderingContext.BLEND_SRC_RGB.toInt
  final val BLEND_DST_ALPHA: Int = dom.raw.WebGLRenderingContext.BLEND_DST_ALPHA.toInt
  final val BLEND_SRC_ALPHA: Int = dom.raw.WebGLRenderingContext.BLEND_SRC_ALPHA.toInt
  final val CONSTANT_COLOR: Int = dom.raw.WebGLRenderingContext.CONSTANT_COLOR.toInt
  final val ONE_MINUS_CONSTANT_COLOR: Int = dom.raw.WebGLRenderingContext.ONE_MINUS_CONSTANT_COLOR.toInt
  final val CONSTANT_ALPHA: Int = dom.raw.WebGLRenderingContext.CONSTANT_ALPHA.toInt
  final val ONE_MINUS_CONSTANT_ALPHA: Int = dom.raw.WebGLRenderingContext.ONE_MINUS_CONSTANT_ALPHA.toInt
  final val BLEND_COLOR: Int = dom.raw.WebGLRenderingContext.BLEND_COLOR.toInt
  final val ARRAY_BUFFER: Int = dom.raw.WebGLRenderingContext.ARRAY_BUFFER.toInt
  final val ELEMENT_ARRAY_BUFFER: Int = dom.raw.WebGLRenderingContext.ELEMENT_ARRAY_BUFFER.toInt
  final val ARRAY_BUFFER_BINDING: Int = dom.raw.WebGLRenderingContext.ARRAY_BUFFER_BINDING.toInt
  final val ELEMENT_ARRAY_BUFFER_BINDING: Int = dom.raw.WebGLRenderingContext.ELEMENT_ARRAY_BUFFER_BINDING.toInt
  final val STREAM_DRAW: Int = dom.raw.WebGLRenderingContext.STREAM_DRAW.toInt
  final val STATIC_DRAW: Int = dom.raw.WebGLRenderingContext.STATIC_DRAW.toInt
  final val DYNAMIC_DRAW: Int = dom.raw.WebGLRenderingContext.DYNAMIC_DRAW.toInt
  final val BUFFER_SIZE: Int = dom.raw.WebGLRenderingContext.BUFFER_SIZE.toInt
  final val BUFFER_USAGE: Int = dom.raw.WebGLRenderingContext.BUFFER_USAGE.toInt
  final val CURRENT_VERTEX_ATTRIB: Int = dom.raw.WebGLRenderingContext.CURRENT_VERTEX_ATTRIB.toInt
  final val FRONT: Int = dom.raw.WebGLRenderingContext.FRONT.toInt
  final val BACK: Int = dom.raw.WebGLRenderingContext.BACK.toInt
  final val FRONT_AND_BACK: Int = dom.raw.WebGLRenderingContext.FRONT_AND_BACK.toInt
  final val CULL_FACE: Int = dom.raw.WebGLRenderingContext.CULL_FACE.toInt
  final val BLEND: Int = dom.raw.WebGLRenderingContext.BLEND.toInt
  final val DITHER: Int = dom.raw.WebGLRenderingContext.DITHER.toInt
  final val STENCIL_TEST: Int = dom.raw.WebGLRenderingContext.STENCIL_TEST.toInt
  final val DEPTH_TEST: Int = dom.raw.WebGLRenderingContext.DEPTH_TEST.toInt
  final val SCISSOR_TEST: Int = dom.raw.WebGLRenderingContext.SCISSOR_TEST.toInt
  final val POLYGON_OFFSET_FILL: Int = dom.raw.WebGLRenderingContext.POLYGON_OFFSET_FILL.toInt
  final val SAMPLE_ALPHA_TO_COVERAGE: Int = dom.raw.WebGLRenderingContext.SAMPLE_ALPHA_TO_COVERAGE.toInt
  final val SAMPLE_COVERAGE: Int = dom.raw.WebGLRenderingContext.SAMPLE_COVERAGE.toInt
  final val NO_ERROR: Int = dom.raw.WebGLRenderingContext.NO_ERROR.toInt
  final val INVALID_ENUM: Int = dom.raw.WebGLRenderingContext.INVALID_ENUM.toInt
  final val INVALID_VALUE: Int = dom.raw.WebGLRenderingContext.INVALID_VALUE.toInt
  final val INVALID_OPERATION: Int = dom.raw.WebGLRenderingContext.INVALID_OPERATION.toInt
  final val OUT_OF_MEMORY: Int = dom.raw.WebGLRenderingContext.OUT_OF_MEMORY.toInt
  final val CW: Int = dom.raw.WebGLRenderingContext.CW.toInt
  final val CCW: Int = dom.raw.WebGLRenderingContext.CCW.toInt
  final val LINE_WIDTH: Int = dom.raw.WebGLRenderingContext.LINE_WIDTH.toInt
  final val ALIASED_POINT_SIZE_RANGE: Int = dom.raw.WebGLRenderingContext.ALIASED_POINT_SIZE_RANGE.toInt
  final val ALIASED_LINE_WIDTH_RANGE: Int = dom.raw.WebGLRenderingContext.ALIASED_LINE_WIDTH_RANGE.toInt
  final val CULL_FACE_MODE: Int = dom.raw.WebGLRenderingContext.CULL_FACE_MODE.toInt
  final val FRONT_FACE: Int = dom.raw.WebGLRenderingContext.FRONT_FACE.toInt
  final val DEPTH_RANGE: Int = dom.raw.WebGLRenderingContext.DEPTH_RANGE.toInt
  final val DEPTH_WRITEMASK: Int = dom.raw.WebGLRenderingContext.DEPTH_WRITEMASK.toInt
  final val DEPTH_CLEAR_VALUE: Int = dom.raw.WebGLRenderingContext.DEPTH_CLEAR_VALUE.toInt
  final val DEPTH_FUNC: Int = dom.raw.WebGLRenderingContext.DEPTH_FUNC.toInt
  final val STENCIL_CLEAR_VALUE: Int = dom.raw.WebGLRenderingContext.STENCIL_CLEAR_VALUE.toInt
  final val STENCIL_FUNC: Int = dom.raw.WebGLRenderingContext.STENCIL_FUNC.toInt
  final val STENCIL_FAIL: Int = dom.raw.WebGLRenderingContext.STENCIL_FAIL.toInt
  final val STENCIL_PASS_DEPTH_FAIL: Int = dom.raw.WebGLRenderingContext.STENCIL_PASS_DEPTH_FAIL.toInt
  final val STENCIL_PASS_DEPTH_PASS: Int = dom.raw.WebGLRenderingContext.STENCIL_PASS_DEPTH_PASS.toInt
  final val STENCIL_REF: Int = dom.raw.WebGLRenderingContext.STENCIL_REF.toInt
  final val STENCIL_VALUE_MASK: Int = dom.raw.WebGLRenderingContext.STENCIL_VALUE_MASK.toInt
  final val STENCIL_WRITEMASK: Int = dom.raw.WebGLRenderingContext.STENCIL_WRITEMASK.toInt
  final val STENCIL_BACK_FUNC: Int = dom.raw.WebGLRenderingContext.STENCIL_BACK_FUNC.toInt
  final val STENCIL_BACK_FAIL: Int = dom.raw.WebGLRenderingContext.STENCIL_BACK_FAIL.toInt
  final val STENCIL_BACK_PASS_DEPTH_FAIL: Int = dom.raw.WebGLRenderingContext.STENCIL_BACK_PASS_DEPTH_FAIL.toInt
  final val STENCIL_BACK_PASS_DEPTH_PASS: Int = dom.raw.WebGLRenderingContext.STENCIL_BACK_PASS_DEPTH_PASS.toInt
  final val STENCIL_BACK_REF: Int = dom.raw.WebGLRenderingContext.STENCIL_BACK_REF.toInt
  final val STENCIL_BACK_VALUE_MASK: Int = dom.raw.WebGLRenderingContext.STENCIL_BACK_VALUE_MASK.toInt
  final val STENCIL_BACK_WRITEMASK: Int = dom.raw.WebGLRenderingContext.STENCIL_BACK_WRITEMASK.toInt
  final val VIEWPORT: Int = dom.raw.WebGLRenderingContext.VIEWPORT.toInt
  final val SCISSOR_BOX: Int = dom.raw.WebGLRenderingContext.SCISSOR_BOX.toInt
  final val COLOR_CLEAR_VALUE: Int = dom.raw.WebGLRenderingContext.COLOR_CLEAR_VALUE.toInt
  final val COLOR_WRITEMASK: Int = dom.raw.WebGLRenderingContext.COLOR_WRITEMASK.toInt
  final val UNPACK_ALIGNMENT: Int = dom.raw.WebGLRenderingContext.UNPACK_ALIGNMENT.toInt
  final val PACK_ALIGNMENT: Int = dom.raw.WebGLRenderingContext.PACK_ALIGNMENT.toInt
  final val MAX_TEXTURE_SIZE: Int = dom.raw.WebGLRenderingContext.MAX_TEXTURE_SIZE.toInt
  final val MAX_VIEWPORT_DIMS: Int = dom.raw.WebGLRenderingContext.MAX_VIEWPORT_DIMS.toInt
  final val SUBPIXEL_BITS: Int = dom.raw.WebGLRenderingContext.SUBPIXEL_BITS.toInt
  final val RED_BITS: Int = dom.raw.WebGLRenderingContext.RED_BITS.toInt
  final val GREEN_BITS: Int = dom.raw.WebGLRenderingContext.GREEN_BITS.toInt
  final val BLUE_BITS: Int = dom.raw.WebGLRenderingContext.BLUE_BITS.toInt
  final val ALPHA_BITS: Int = dom.raw.WebGLRenderingContext.ALPHA_BITS.toInt
  final val DEPTH_BITS: Int = dom.raw.WebGLRenderingContext.DEPTH_BITS.toInt
  final val STENCIL_BITS: Int = dom.raw.WebGLRenderingContext.STENCIL_BITS.toInt
  final val POLYGON_OFFSET_UNITS: Int = dom.raw.WebGLRenderingContext.POLYGON_OFFSET_UNITS.toInt
  final val POLYGON_OFFSET_FACTOR: Int = dom.raw.WebGLRenderingContext.POLYGON_OFFSET_FACTOR.toInt
  final val TEXTURE_BINDING_2D: Int = dom.raw.WebGLRenderingContext.TEXTURE_BINDING_2D.toInt
  final val SAMPLE_BUFFERS: Int = dom.raw.WebGLRenderingContext.SAMPLE_BUFFERS.toInt
  final val SAMPLES: Int = dom.raw.WebGLRenderingContext.SAMPLES.toInt
  final val SAMPLE_COVERAGE_VALUE: Int = dom.raw.WebGLRenderingContext.SAMPLE_COVERAGE_VALUE.toInt
  final val SAMPLE_COVERAGE_INVERT: Int = dom.raw.WebGLRenderingContext.SAMPLE_COVERAGE_INVERT.toInt
  final val COMPRESSED_TEXTURE_FORMATS: Int = dom.raw.WebGLRenderingContext.COMPRESSED_TEXTURE_FORMATS.toInt
  final val DONT_CARE: Int = dom.raw.WebGLRenderingContext.DONT_CARE.toInt
  final val FASTEST: Int = dom.raw.WebGLRenderingContext.FASTEST.toInt
  final val NICEST: Int = dom.raw.WebGLRenderingContext.NICEST.toInt
  final val GENERATE_MIPMAP_HINT: Int = dom.raw.WebGLRenderingContext.GENERATE_MIPMAP_HINT.toInt
  final val BYTE: Int = dom.raw.WebGLRenderingContext.BYTE.toInt
  final val UNSIGNED_BYTE: Int = dom.raw.WebGLRenderingContext.UNSIGNED_BYTE.toInt
  final val SHORT: Int = dom.raw.WebGLRenderingContext.SHORT.toInt
  final val UNSIGNED_SHORT: Int = dom.raw.WebGLRenderingContext.UNSIGNED_SHORT.toInt
  final val INT: Int = dom.raw.WebGLRenderingContext.INT.toInt
  final val UNSIGNED_INT: Int = dom.raw.WebGLRenderingContext.UNSIGNED_INT.toInt
  final val FLOAT: Int = dom.raw.WebGLRenderingContext.FLOAT.toInt
  final val DEPTH_COMPONENT: Int = dom.raw.WebGLRenderingContext.DEPTH_COMPONENT.toInt
  final val ALPHA: Int = dom.raw.WebGLRenderingContext.ALPHA.toInt
  final val RGB: Int = dom.raw.WebGLRenderingContext.RGB.toInt
  final val RGBA: Int = dom.raw.WebGLRenderingContext.RGBA.toInt
  final val LUMINANCE: Int = dom.raw.WebGLRenderingContext.LUMINANCE.toInt
  final val LUMINANCE_ALPHA: Int = dom.raw.WebGLRenderingContext.LUMINANCE_ALPHA.toInt
  final val UNSIGNED_SHORT_4_4_4_4: Int = dom.raw.WebGLRenderingContext.UNSIGNED_SHORT_4_4_4_4.toInt
  final val UNSIGNED_SHORT_5_5_5_1: Int = dom.raw.WebGLRenderingContext.UNSIGNED_SHORT_5_5_5_1.toInt
  final val UNSIGNED_SHORT_5_6_5: Int = dom.raw.WebGLRenderingContext.UNSIGNED_SHORT_5_6_5.toInt
  final val FRAGMENT_SHADER: Int = dom.raw.WebGLRenderingContext.FRAGMENT_SHADER.toInt
  final val VERTEX_SHADER: Int = dom.raw.WebGLRenderingContext.VERTEX_SHADER.toInt
  final val MAX_VERTEX_ATTRIBS: Int = dom.raw.WebGLRenderingContext.MAX_VERTEX_ATTRIBS.toInt
  final val MAX_VERTEX_UNIFORM_VECTORS: Int = dom.raw.WebGLRenderingContext.MAX_VERTEX_UNIFORM_VECTORS.toInt
  final val MAX_VARYING_VECTORS: Int = dom.raw.WebGLRenderingContext.MAX_VARYING_VECTORS.toInt
  final val MAX_COMBINED_TEXTURE_IMAGE_UNITS: Int = dom.raw.WebGLRenderingContext.MAX_COMBINED_TEXTURE_IMAGE_UNITS.toInt
  final val MAX_VERTEX_TEXTURE_IMAGE_UNITS: Int = dom.raw.WebGLRenderingContext.MAX_VERTEX_TEXTURE_IMAGE_UNITS.toInt
  final val MAX_TEXTURE_IMAGE_UNITS: Int = dom.raw.WebGLRenderingContext.MAX_TEXTURE_IMAGE_UNITS.toInt
  final val MAX_FRAGMENT_UNIFORM_VECTORS: Int = dom.raw.WebGLRenderingContext.MAX_FRAGMENT_UNIFORM_VECTORS.toInt
  final val SHADER_TYPE: Int = dom.raw.WebGLRenderingContext.SHADER_TYPE.toInt
  final val DELETE_STATUS: Int = dom.raw.WebGLRenderingContext.DELETE_STATUS.toInt
  final val LINK_STATUS: Int = dom.raw.WebGLRenderingContext.LINK_STATUS.toInt
  final val VALIDATE_STATUS: Int = dom.raw.WebGLRenderingContext.VALIDATE_STATUS.toInt
  final val ATTACHED_SHADERS: Int = dom.raw.WebGLRenderingContext.ATTACHED_SHADERS.toInt
  final val ACTIVE_UNIFORMS: Int = dom.raw.WebGLRenderingContext.ACTIVE_UNIFORMS.toInt
  final val ACTIVE_ATTRIBUTES: Int = dom.raw.WebGLRenderingContext.ACTIVE_ATTRIBUTES.toInt
  final val SHADING_LANGUAGE_VERSION: Int = dom.raw.WebGLRenderingContext.SHADING_LANGUAGE_VERSION.toInt
  final val CURRENT_PROGRAM: Int = dom.raw.WebGLRenderingContext.CURRENT_PROGRAM.toInt
  final val NEVER: Int = dom.raw.WebGLRenderingContext.NEVER.toInt
  final val LESS: Int = dom.raw.WebGLRenderingContext.LESS.toInt
  final val EQUAL: Int = dom.raw.WebGLRenderingContext.EQUAL.toInt
  final val LEQUAL: Int = dom.raw.WebGLRenderingContext.LEQUAL.toInt
  final val GREATER: Int = dom.raw.WebGLRenderingContext.GREATER.toInt
  final val NOTEQUAL: Int = dom.raw.WebGLRenderingContext.NOTEQUAL.toInt
  final val GEQUAL: Int = dom.raw.WebGLRenderingContext.GEQUAL.toInt
  final val ALWAYS: Int = dom.raw.WebGLRenderingContext.ALWAYS.toInt
  final val KEEP: Int = dom.raw.WebGLRenderingContext.KEEP.toInt
  final val REPLACE: Int = dom.raw.WebGLRenderingContext.REPLACE.toInt
  final val INCR: Int = dom.raw.WebGLRenderingContext.INCR.toInt
  final val DECR: Int = dom.raw.WebGLRenderingContext.DECR.toInt
  final val INVERT: Int = dom.raw.WebGLRenderingContext.INVERT.toInt
  final val INCR_WRAP: Int = dom.raw.WebGLRenderingContext.INCR_WRAP.toInt
  final val DECR_WRAP: Int = dom.raw.WebGLRenderingContext.DECR_WRAP.toInt
  final val VENDOR: Int = dom.raw.WebGLRenderingContext.VENDOR.toInt
  final val RENDERER: Int = dom.raw.WebGLRenderingContext.RENDERER.toInt
  final val VERSION: Int = dom.raw.WebGLRenderingContext.VERSION.toInt
  final val NEAREST: Int = dom.raw.WebGLRenderingContext.NEAREST.toInt
  final val LINEAR: Int = dom.raw.WebGLRenderingContext.LINEAR.toInt
  final val NEAREST_MIPMAP_NEAREST: Int = dom.raw.WebGLRenderingContext.NEAREST_MIPMAP_NEAREST.toInt
  final val LINEAR_MIPMAP_NEAREST: Int = dom.raw.WebGLRenderingContext.LINEAR_MIPMAP_NEAREST.toInt
  final val NEAREST_MIPMAP_LINEAR: Int = dom.raw.WebGLRenderingContext.NEAREST_MIPMAP_LINEAR.toInt
  final val LINEAR_MIPMAP_LINEAR: Int = dom.raw.WebGLRenderingContext.LINEAR_MIPMAP_LINEAR.toInt
  final val TEXTURE_MAG_FILTER: Int = dom.raw.WebGLRenderingContext.TEXTURE_MAG_FILTER.toInt
  final val TEXTURE_MIN_FILTER: Int = dom.raw.WebGLRenderingContext.TEXTURE_MIN_FILTER.toInt
  final val TEXTURE_WRAP_S: Int = dom.raw.WebGLRenderingContext.TEXTURE_WRAP_S.toInt
  final val TEXTURE_WRAP_T: Int = dom.raw.WebGLRenderingContext.TEXTURE_WRAP_T.toInt
  final val TEXTURE_2D: Int = dom.raw.WebGLRenderingContext.TEXTURE_2D.toInt
  final val TEXTURE: Int = dom.raw.WebGLRenderingContext.TEXTURE.toInt
  final val TEXTURE_CUBE_MAP: Int = dom.raw.WebGLRenderingContext.TEXTURE_CUBE_MAP.toInt
  final val TEXTURE_BINDING_CUBE_MAP: Int = dom.raw.WebGLRenderingContext.TEXTURE_BINDING_CUBE_MAP.toInt
  final val TEXTURE_CUBE_MAP_POSITIVE_X: Int = dom.raw.WebGLRenderingContext.TEXTURE_CUBE_MAP_POSITIVE_X.toInt
  final val TEXTURE_CUBE_MAP_NEGATIVE_X: Int = dom.raw.WebGLRenderingContext.TEXTURE_CUBE_MAP_NEGATIVE_X.toInt
  final val TEXTURE_CUBE_MAP_POSITIVE_Y: Int = dom.raw.WebGLRenderingContext.TEXTURE_CUBE_MAP_POSITIVE_Y.toInt
  final val TEXTURE_CUBE_MAP_NEGATIVE_Y: Int = dom.raw.WebGLRenderingContext.TEXTURE_CUBE_MAP_NEGATIVE_Y.toInt
  final val TEXTURE_CUBE_MAP_POSITIVE_Z: Int = dom.raw.WebGLRenderingContext.TEXTURE_CUBE_MAP_POSITIVE_Z.toInt
  final val TEXTURE_CUBE_MAP_NEGATIVE_Z: Int = dom.raw.WebGLRenderingContext.TEXTURE_CUBE_MAP_NEGATIVE_Z.toInt
  final val MAX_CUBE_MAP_TEXTURE_SIZE: Int = dom.raw.WebGLRenderingContext.MAX_CUBE_MAP_TEXTURE_SIZE.toInt
  final val TEXTURE0: Int = dom.raw.WebGLRenderingContext.TEXTURE0.toInt
  final val TEXTURE1: Int = dom.raw.WebGLRenderingContext.TEXTURE1.toInt
  final val TEXTURE2: Int = dom.raw.WebGLRenderingContext.TEXTURE2.toInt
  final val TEXTURE3: Int = dom.raw.WebGLRenderingContext.TEXTURE3.toInt
  final val TEXTURE4: Int = dom.raw.WebGLRenderingContext.TEXTURE4.toInt
  final val TEXTURE5: Int = dom.raw.WebGLRenderingContext.TEXTURE5.toInt
  final val TEXTURE6: Int = dom.raw.WebGLRenderingContext.TEXTURE6.toInt
  final val TEXTURE7: Int = dom.raw.WebGLRenderingContext.TEXTURE7.toInt
  final val TEXTURE8: Int = dom.raw.WebGLRenderingContext.TEXTURE8.toInt
  final val TEXTURE9: Int = dom.raw.WebGLRenderingContext.TEXTURE9.toInt
  final val TEXTURE10: Int = dom.raw.WebGLRenderingContext.TEXTURE10.toInt
  final val TEXTURE11: Int = dom.raw.WebGLRenderingContext.TEXTURE11.toInt
  final val TEXTURE12: Int = dom.raw.WebGLRenderingContext.TEXTURE12.toInt
  final val TEXTURE13: Int = dom.raw.WebGLRenderingContext.TEXTURE13.toInt
  final val TEXTURE14: Int = dom.raw.WebGLRenderingContext.TEXTURE14.toInt
  final val TEXTURE15: Int = dom.raw.WebGLRenderingContext.TEXTURE15.toInt
  final val TEXTURE16: Int = dom.raw.WebGLRenderingContext.TEXTURE16.toInt
  final val TEXTURE17: Int = dom.raw.WebGLRenderingContext.TEXTURE17.toInt
  final val TEXTURE18: Int = dom.raw.WebGLRenderingContext.TEXTURE18.toInt
  final val TEXTURE19: Int = dom.raw.WebGLRenderingContext.TEXTURE19.toInt
  final val TEXTURE20: Int = dom.raw.WebGLRenderingContext.TEXTURE20.toInt
  final val TEXTURE21: Int = dom.raw.WebGLRenderingContext.TEXTURE21.toInt
  final val TEXTURE22: Int = dom.raw.WebGLRenderingContext.TEXTURE22.toInt
  final val TEXTURE23: Int = dom.raw.WebGLRenderingContext.TEXTURE23.toInt
  final val TEXTURE24: Int = dom.raw.WebGLRenderingContext.TEXTURE24.toInt
  final val TEXTURE25: Int = dom.raw.WebGLRenderingContext.TEXTURE25.toInt
  final val TEXTURE26: Int = dom.raw.WebGLRenderingContext.TEXTURE26.toInt
  final val TEXTURE27: Int = dom.raw.WebGLRenderingContext.TEXTURE27.toInt
  final val TEXTURE28: Int = dom.raw.WebGLRenderingContext.TEXTURE28.toInt
  final val TEXTURE29: Int = dom.raw.WebGLRenderingContext.TEXTURE29.toInt
  final val TEXTURE30: Int = dom.raw.WebGLRenderingContext.TEXTURE30.toInt
  final val TEXTURE31: Int = dom.raw.WebGLRenderingContext.TEXTURE31.toInt
  final val ACTIVE_TEXTURE: Int = dom.raw.WebGLRenderingContext.ACTIVE_TEXTURE.toInt
  final val REPEAT: Int = dom.raw.WebGLRenderingContext.REPEAT.toInt
  final val CLAMP_TO_EDGE: Int = dom.raw.WebGLRenderingContext.CLAMP_TO_EDGE.toInt
  final val MIRRORED_REPEAT: Int = dom.raw.WebGLRenderingContext.MIRRORED_REPEAT.toInt
  final val FLOAT_VEC2: Int = dom.raw.WebGLRenderingContext.FLOAT_VEC2.toInt
  final val FLOAT_VEC3: Int = dom.raw.WebGLRenderingContext.FLOAT_VEC3.toInt
  final val FLOAT_VEC4: Int = dom.raw.WebGLRenderingContext.FLOAT_VEC4.toInt
  final val INT_VEC2: Int = dom.raw.WebGLRenderingContext.INT_VEC2.toInt
  final val INT_VEC3: Int = dom.raw.WebGLRenderingContext.INT_VEC3.toInt
  final val INT_VEC4: Int = dom.raw.WebGLRenderingContext.INT_VEC4.toInt
  final val BOOL: Int = dom.raw.WebGLRenderingContext.BOOL.toInt
  final val BOOL_VEC2: Int = dom.raw.WebGLRenderingContext.BOOL_VEC2.toInt
  final val BOOL_VEC3: Int = dom.raw.WebGLRenderingContext.BOOL_VEC3.toInt
  final val BOOL_VEC4: Int = dom.raw.WebGLRenderingContext.BOOL_VEC4.toInt
  final val FLOAT_MAT2: Int = dom.raw.WebGLRenderingContext.FLOAT_MAT2.toInt
  final val FLOAT_MAT3: Int = dom.raw.WebGLRenderingContext.FLOAT_MAT3.toInt
  final val FLOAT_MAT4: Int = dom.raw.WebGLRenderingContext.FLOAT_MAT4.toInt
  final val SAMPLER_2D: Int = dom.raw.WebGLRenderingContext.SAMPLER_2D.toInt
  final val SAMPLER_CUBE: Int = dom.raw.WebGLRenderingContext.SAMPLER_CUBE.toInt
  final val VERTEX_ATTRIB_ARRAY_ENABLED: Int = dom.raw.WebGLRenderingContext.VERTEX_ATTRIB_ARRAY_ENABLED.toInt
  final val VERTEX_ATTRIB_ARRAY_SIZE: Int = dom.raw.WebGLRenderingContext.VERTEX_ATTRIB_ARRAY_SIZE.toInt
  final val VERTEX_ATTRIB_ARRAY_STRIDE: Int = dom.raw.WebGLRenderingContext.VERTEX_ATTRIB_ARRAY_STRIDE.toInt
  final val VERTEX_ATTRIB_ARRAY_TYPE: Int = dom.raw.WebGLRenderingContext.VERTEX_ATTRIB_ARRAY_TYPE.toInt
  final val VERTEX_ATTRIB_ARRAY_NORMALIZED: Int = dom.raw.WebGLRenderingContext.VERTEX_ATTRIB_ARRAY_NORMALIZED.toInt
  final val VERTEX_ATTRIB_ARRAY_POINTER: Int = dom.raw.WebGLRenderingContext.VERTEX_ATTRIB_ARRAY_POINTER.toInt
  final val VERTEX_ATTRIB_ARRAY_BUFFER_BINDING: Int = dom.raw.WebGLRenderingContext.VERTEX_ATTRIB_ARRAY_BUFFER_BINDING.toInt
  final val COMPILE_STATUS: Int = dom.raw.WebGLRenderingContext.COMPILE_STATUS.toInt
  final val LOW_FLOAT: Int = dom.raw.WebGLRenderingContext.LOW_FLOAT.toInt
  final val MEDIUM_FLOAT: Int = dom.raw.WebGLRenderingContext.MEDIUM_FLOAT.toInt
  final val HIGH_FLOAT: Int = dom.raw.WebGLRenderingContext.HIGH_FLOAT.toInt
  final val LOW_INT: Int = dom.raw.WebGLRenderingContext.LOW_INT.toInt
  final val MEDIUM_INT: Int = dom.raw.WebGLRenderingContext.MEDIUM_INT.toInt
  final val HIGH_INT: Int = dom.raw.WebGLRenderingContext.HIGH_INT.toInt
  final val FRAMEBUFFER: Int = dom.raw.WebGLRenderingContext.FRAMEBUFFER.toInt
  final val RENDERBUFFER: Int = dom.raw.WebGLRenderingContext.RENDERBUFFER.toInt
  final val RGBA4: Int = dom.raw.WebGLRenderingContext.RGBA4.toInt
  final val RGB5_A1: Int = dom.raw.WebGLRenderingContext.RGB5_A1.toInt
  final val RGB565: Int = dom.raw.WebGLRenderingContext.RGB565.toInt
  final val DEPTH_COMPONENT16: Int = dom.raw.WebGLRenderingContext.DEPTH_COMPONENT16.toInt
  final val STENCIL_INDEX: Int = dom.raw.WebGLRenderingContext.STENCIL_INDEX.toInt
  final val STENCIL_INDEX8: Int = dom.raw.WebGLRenderingContext.STENCIL_INDEX8.toInt
  final val DEPTH_STENCIL: Int = dom.raw.WebGLRenderingContext.DEPTH_STENCIL.toInt
  final val RENDERBUFFER_WIDTH: Int = dom.raw.WebGLRenderingContext.RENDERBUFFER_WIDTH.toInt
  final val RENDERBUFFER_HEIGHT: Int = dom.raw.WebGLRenderingContext.RENDERBUFFER_HEIGHT.toInt
  final val RENDERBUFFER_INTERNAL_FORMAT: Int = dom.raw.WebGLRenderingContext.RENDERBUFFER_INTERNAL_FORMAT.toInt
  final val RENDERBUFFER_RED_SIZE: Int = dom.raw.WebGLRenderingContext.RENDERBUFFER_RED_SIZE.toInt
  final val RENDERBUFFER_GREEN_SIZE: Int = dom.raw.WebGLRenderingContext.RENDERBUFFER_GREEN_SIZE.toInt
  final val RENDERBUFFER_BLUE_SIZE: Int = dom.raw.WebGLRenderingContext.RENDERBUFFER_BLUE_SIZE.toInt
  final val RENDERBUFFER_ALPHA_SIZE: Int = dom.raw.WebGLRenderingContext.RENDERBUFFER_ALPHA_SIZE.toInt
  final val RENDERBUFFER_DEPTH_SIZE: Int = dom.raw.WebGLRenderingContext.RENDERBUFFER_DEPTH_SIZE.toInt
  final val RENDERBUFFER_STENCIL_SIZE: Int = dom.raw.WebGLRenderingContext.RENDERBUFFER_STENCIL_SIZE.toInt
  final val FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE: Int = dom.raw.WebGLRenderingContext.FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE.toInt
  final val FRAMEBUFFER_ATTACHMENT_OBJECT_NAME: Int = dom.raw.WebGLRenderingContext.FRAMEBUFFER_ATTACHMENT_OBJECT_NAME.toInt
  final val FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL: Int = dom.raw.WebGLRenderingContext.FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL.toInt
  final val FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE: Int = dom.raw.WebGLRenderingContext.FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE.toInt
  final val COLOR_ATTACHMENT0: Int = dom.raw.WebGLRenderingContext.COLOR_ATTACHMENT0.toInt
  final val DEPTH_ATTACHMENT: Int = dom.raw.WebGLRenderingContext.DEPTH_ATTACHMENT.toInt
  final val STENCIL_ATTACHMENT: Int = dom.raw.WebGLRenderingContext.STENCIL_ATTACHMENT.toInt
  final val DEPTH_STENCIL_ATTACHMENT: Int = dom.raw.WebGLRenderingContext.DEPTH_STENCIL_ATTACHMENT.toInt
  final val NONE: Int = dom.raw.WebGLRenderingContext.NONE.toInt
  final val FRAMEBUFFER_COMPLETE: Int = dom.raw.WebGLRenderingContext.FRAMEBUFFER_COMPLETE.toInt
  final val FRAMEBUFFER_INCOMPLETE_ATTACHMENT: Int = dom.raw.WebGLRenderingContext.FRAMEBUFFER_INCOMPLETE_ATTACHMENT.toInt
  final val FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT: Int = dom.raw.WebGLRenderingContext.FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT.toInt
  final val FRAMEBUFFER_INCOMPLETE_DIMENSIONS: Int = dom.raw.WebGLRenderingContext.FRAMEBUFFER_INCOMPLETE_DIMENSIONS.toInt
  final val FRAMEBUFFER_UNSUPPORTED: Int = dom.raw.WebGLRenderingContext.FRAMEBUFFER_UNSUPPORTED.toInt
  final val FRAMEBUFFER_BINDING: Int = dom.raw.WebGLRenderingContext.FRAMEBUFFER_BINDING.toInt
  final val RENDERBUFFER_BINDING: Int = dom.raw.WebGLRenderingContext.RENDERBUFFER_BINDING.toInt
  final val MAX_RENDERBUFFER_SIZE: Int = dom.raw.WebGLRenderingContext.MAX_RENDERBUFFER_SIZE.toInt
  final val INVALID_FRAMEBUFFER_OPERATION: Int = dom.raw.WebGLRenderingContext.INVALID_FRAMEBUFFER_OPERATION.toInt

  /* public API - methods */

  final def createByteBuffer(sz: Int): ByteBuffer = {
    ByteBuffer.allocateDirect(sz).order(ByteOrder.nativeOrder())
  }

  final def createShortBuffer(sz: Int): ShortBuffer = {
    ByteBuffer.allocateDirect(sz * this.bytesPerShort).order(ByteOrder.nativeOrder()).asShortBuffer()
  }

  final def createIntBuffer(sz: Int): IntBuffer = {
    ByteBuffer.allocateDirect(sz * this.bytesPerInt).order(ByteOrder.nativeOrder()).asIntBuffer()
  }

  final def createFloatBuffer(sz: Int): FloatBuffer = {
    ByteBuffer.allocateDirect(sz * this.bytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer()
  }

  final def createDoubleBuffer(sz: Int): DoubleBuffer = {
    ByteBuffer.allocateDirect(sz * this.bytesPerDouble).order(ByteOrder.nativeOrder()).asDoubleBuffer()
  }

  /* public API - implicits */

  //implicit val default = new Macrogl()

  /* implementation-specific methods */

}

