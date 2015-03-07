package games.opengl

import java.nio.{ByteBuffer, ShortBuffer, IntBuffer, FloatBuffer, DoubleBuffer}

import org.lwjgl.opengl._
import org.lwjgl.util.glu._

// Auxiliary components

object Token {

  type Buffer = Int

  object Buffer {
    val invalid: Buffer = -1
    val none: Buffer = 0
  }

  type Program = Int

  object Program {
    val invalid: Program = 0
  }

  type Shader = Int

  object Shader {
    val invalid: Shader = 0
  }

  type UniformLocation = Int

  object UniformLocation {
    val invalid: UniformLocation = -1
  }

  type FrameBuffer = Int

  object FrameBuffer {
    val invalid: FrameBuffer = -1
    val none: FrameBuffer = 0
  }

  type RenderBuffer = Int

  object RenderBuffer {
    val invalid: RenderBuffer = -1
    val none: RenderBuffer = 0
  }

  type Texture = Int

  object Texture {
    val invalid: Texture = -1
    val none: Texture = 0
  }

}

// Main componenents

class GLES2LWJGL() extends GLES2 {

  /* public API */

  final def activeTexture(texture: Int): Unit = {
    GL13.glActiveTexture(texture)
  }

  final def attachShader(program: Token.Program, shader: Token.Shader): Unit = {
    GL20.glAttachShader(program, shader)
  }

  final def bindAttribLocation(program: Token.Program, index: Int, name: String): Unit = {
    GL20.glBindAttribLocation(program, index, name)
  }

  final def bindBuffer(target: Int, buffer: Token.Buffer): Unit = {
    GL15.glBindBuffer(target, buffer)
  }

  final def bindFramebuffer(target: Int, framebuffer: Token.FrameBuffer): Unit = {
    GL30.glBindFramebuffer(target, framebuffer)
  }

  final def bindRenderbuffer(target: Int, renderbuffer: Token.RenderBuffer): Unit = {
    GL30.glBindRenderbuffer(target, renderbuffer)
  }

  final def bindTexture(target: Int, texture: Token.Texture): Unit = {
    GL11.glBindTexture(target, texture)
  }

  final def blendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = {
    GL14.glBlendColor(red, green, blue, alpha)
  }

  final def blendEquation(mode: Int): Unit = {
    GL14.glBlendEquation(mode)
  }

  final def blendEquationSeparate(modeRGB: Int, modeAlpha: Int): Unit = {
    GL20.glBlendEquationSeparate(modeRGB, modeAlpha)
  }

  final def blendFunc(sfactor: Int, dfactor: Int): Unit = {
    GL11.glBlendFunc(sfactor, dfactor)
  }

  final def blendFuncSeparate(srcfactorRGB: Int, dstfactorRGB: Int, srcfactorAlpha: Int, dstfactorAlpha: Int): Unit = {
    GL14.glBlendFuncSeparate(srcfactorRGB, dstfactorRGB, srcfactorAlpha, dstfactorAlpha)
  }

  final def bufferData(target: Int, totalBytes: Long, usage: Int): Unit = {
    GL15.glBufferData(target, totalBytes, usage)
  }

  final def bufferData(target: Int, data: ByteBuffer, usage: Int): Unit = {
    GL15.glBufferData(target, data, usage)
  }
  final def bufferData(target: Int, data: ShortBuffer, usage: Int): Unit = {
    GL15.glBufferData(target, data, usage)
  }
  final def bufferData(target: Int, data: IntBuffer, usage: Int): Unit = {
    GL15.glBufferData(target, data, usage)
  }
  final def bufferData(target: Int, data: FloatBuffer, usage: Int): Unit = {
    GL15.glBufferData(target, data, usage)
  }
  final def bufferData(target: Int, data: DoubleBuffer, usage: Int): Unit = {
    GL15.glBufferData(target, data, usage)
  }

  final def bufferSubData(target: Int, offset: Long, data: ByteBuffer): Unit = {
    GL15.glBufferSubData(target, offset, data)
  }
  final def bufferSubData(target: Int, offset: Long, data: ShortBuffer): Unit = {
    GL15.glBufferSubData(target, offset, data)
  }
  final def bufferSubData(target: Int, offset: Long, data: IntBuffer): Unit = {
    GL15.glBufferSubData(target, offset, data)
  }
  final def bufferSubData(target: Int, offset: Long, data: FloatBuffer): Unit = {
    GL15.glBufferSubData(target, offset, data)
  }
  final def bufferSubData(target: Int, offset: Long, data: DoubleBuffer): Unit = {
    GL15.glBufferSubData(target, offset, data)
  }

  final def checkFramebufferStatus(target: Int): Int = {
    GL30.glCheckFramebufferStatus(target)
  }

  final def clear(mask: Int): Unit = {
    GL11.glClear(mask)
  }

  final def clearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = {
    GL11.glClearColor(red, green, blue, alpha)
  }

  final def clearDepth(depth: Double): Unit = {
    GL11.glClearDepth(depth)
  }

  final def clearStencil(s: Int): Unit = {
    GL11.glClearStencil(s)
  }

  final def colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit = {
    GL11.glColorMask(red, green, blue, alpha)
  }

  final def compileShader(shader: Token.Shader): Unit = {
    GL20.glCompileShader(shader)
  }

  final def compressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
    data: ByteBuffer): Unit = {
    GL13.glCompressedTexImage2D(target, level, internalformat, width, height, border, data)
  }

  final def compressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
    format: Int, data: ByteBuffer): Unit = {
    GL13.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, data)
  }

  final def copyTexImage2D(target: Int, level: Int, internalFormat: Int, x: Int, y: Int, width: Int, height: Int, border: Int): Unit = {
    GL11.glCopyTexImage2D(target, level, internalFormat, x, y, width, height, border)
  }

  final def copyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit = {
    GL11.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)
  }

  final def createBuffer(): Token.Buffer = {
    GL15.glGenBuffers()
  }

  final def createFramebuffer(): Token.FrameBuffer = {
    GL30.glGenFramebuffers()
  }

  final def createProgram(): Token.Program = {
    GL20.glCreateProgram()
  }

  final def createRenderbuffer(): Token.RenderBuffer = {
    GL30.glGenRenderbuffers()
  }

  final def createShader(`type`: Int): Token.Shader = {
    GL20.glCreateShader(`type`)
  }

  final def createTexture(): Token.Texture = {
    GL11.glGenTextures()
  }

  final def cullFace(mode: Int): Unit = {
    GL11.glCullFace(mode)
  }

  final def deleteBuffer(buffer: Token.Buffer): Unit = {
    GL15.glDeleteBuffers(buffer)
  }

  final def deleteFramebuffer(framebuffer: Token.FrameBuffer): Unit = {
    GL30.glDeleteFramebuffers(framebuffer)
  }

  final def deleteProgram(program: Token.Program): Unit = {
    GL20.glDeleteProgram(program)
  }

  final def deleteRenderbuffer(renderbuffer: Token.RenderBuffer): Unit = {
    GL30.glDeleteRenderbuffers(renderbuffer)
  }

  final def deleteShader(shader: Token.Shader): Unit = {
    GL20.glDeleteShader(shader)
  }

  final def deleteTexture(texture: Token.Texture): Unit = {
    GL11.glDeleteTextures(texture)
  }

  final def depthFunc(func: Int): Unit = {
    GL11.glDepthFunc(func)
  }

  final def depthMask(flag: Boolean): Unit = {
    GL11.glDepthMask(flag)
  }

  final def depthRange(zNear: Double, zFar: Double): Unit = {
    GL11.glDepthRange(zNear, zFar)
  }

  final def detachShader(program: Token.Program, shader: Token.Shader): Unit = {
    GL20.glDetachShader(program, shader)
  }

  final def disable(cap: Int): Unit = {
    GL11.glDisable(cap)
  }

  final def disableVertexAttribArray(index: Int): Unit = {
    GL20.glDisableVertexAttribArray(index)
  }

  final def drawArrays(mode: Int, first: Int, count: Int): Unit = {
    GL11.glDrawArrays(mode, first, count)
  }

  final def drawElements(mode: Int, count: Int, `type`: Int, offset: Long): Unit = {
    // may be a good idea to check that an element array buffer is currently bound
    GL11.glDrawElements(mode, count, `type`, offset)
  }

  final def enable(cap: Int): Unit = {
    GL11.glEnable(cap)
  }

  final def enableVertexAttribArray(index: Int): Unit = {
    GL20.glEnableVertexAttribArray(index)
  }

  final def finish(): Unit = {
    GL11.glFinish()
  }

  final def flush(): Unit = {
    GL11.glFlush()
  }

  final def framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Token.RenderBuffer): Unit = {
    GL30.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)
  }

  final def framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Token.Texture, level: Int): Unit = {
    GL30.glFramebufferTexture2D(target, attachment, textarget, texture, level)
  }

  final def frontFace(mode: Int): Unit = {
    GL11.glFrontFace(mode)
  }

  final def generateMipmap(target: Int): Unit = {
    GL30.glGenerateMipmap(target)
  }

  final def getActiveAttrib(program: Token.Program, index: Int): ActiveInfo = {
    val nameMaxSize = this.getProgramParameteri(program, GL20.GL_ACTIVE_ATTRIBUTE_MAX_LENGTH)
    this.tmpInt.clear()
    val name = GL20.glGetActiveAttrib(program, index, nameMaxSize, this.tmpInt)
    ActiveInfo(this.tmpInt.get(0), this.tmpInt.get(1), name)
  }

  final def getActiveUniform(program: Token.Program, index: Int): ActiveInfo = {
    val nameMaxSize = this.getProgramParameteri(program, GL20.GL_ACTIVE_UNIFORM_MAX_LENGTH)
    this.tmpInt.clear()
    val name = GL20.glGetActiveUniform(program, index, nameMaxSize, this.tmpInt)
    ActiveInfo(this.tmpInt.get(0), this.tmpInt.get(1), name)
  }

  final def getAttachedShaders(program: Token.Program): Array[Token.Shader] = {
    val maxCount = this.getProgramParameteri(program, GL20.GL_ATTACHED_SHADERS)
    val buffer = GLES2.createIntData(maxCount)
    this.tmpInt.clear()
    GL20.glGetAttachedShaders(program, this.tmpInt, buffer)
    val count = this.tmpInt.get(0)
    val array = new Array[Token.Shader](count)
    for (i <- 0 until count) {
      array(i) = buffer.get(i)
    }
    array
  }

  final def getAttribLocation(program: Token.Program, name: String): Int = {
    GL20.glGetAttribLocation(program, name)
  }

  final def getBufferParameteri(target: Int, pname: Int): Int = {
    GL15.glGetBufferParameteri(target, pname)
  }

  final def getParameterBuffer(pname: Int): Token.Buffer = {
    GL11.glGetInteger(pname)
  }

  final def getParameterTexture(pname: Int): Token.Texture = {
    GL11.glGetInteger(pname)
  }

  final def getParameterFramebuffer(pname: Int): Token.FrameBuffer = {
    GL11.glGetInteger(pname)
  }

  final def getParameterProgram(pname: Int): Token.Program = {
    GL11.glGetInteger(pname)
  }

  final def getParameterRenderbuffer(pname: Int): Token.RenderBuffer = {
    GL11.glGetInteger(pname)
  }

  final def getParameterShader(pname: Int): Token.Shader = {
    GL11.glGetInteger(pname)
  }

  final def getParameterString(pname: Int): String = {
    GL11.glGetString(pname)
  }

  final def getParameteri(pname: Int): Int = {
    GL11.glGetInteger(pname)
  }

  final def getParameteriv(pname: Int, outputs: IntBuffer): Unit = {
    GL11.glGetInteger(pname, outputs)
  }

  final def getParameterf(pname: Int): Float = {
    GL11.glGetFloat(pname)
  }

  final def getParameterfv(pname: Int, outputs: FloatBuffer): Unit = {
    GL11.glGetFloat(pname, outputs)
  }

  final def getParameterb(pname: Int): Boolean = {
    GL11.glGetBoolean(pname)
  }

  final def getParameterbv(pname: Int, outputs: ByteBuffer): Unit = {
    GL11.glGetBoolean(pname, outputs)
  }

  final def getError(): Int = {
    GL11.glGetError()
  }

  final def getFramebufferAttachmentParameteri(target: Int, attachment: Int, pname: Int): Int = {
    GL30.glGetFramebufferAttachmentParameteri(target, attachment, pname)
  }

  final def getFramebufferAttachmentParameterRenderbuffer(target: Int, attachment: Int, pname: Int): Token.RenderBuffer = {
    GL30.glGetFramebufferAttachmentParameteri(target, attachment, pname)
  }

  final def getFramebufferAttachmentParameterTexture(target: Int, attachment: Int, pname: Int): Token.Texture = {
    GL30.glGetFramebufferAttachmentParameteri(target, attachment, pname)
  }

  final def getProgramParameteri(program: Token.Program, pname: Int): Int = {
    GL20.glGetProgrami(program, pname)
  }

  final def getProgramParameterb(program: Token.Program, pname: Int): Boolean = {
    GL20.glGetProgrami(program, pname) != GLES2.FALSE
  }

  final def getProgramInfoLog(program: Token.Program): String = {
    val infoLogLength = this.getProgramParameteri(program, GL20.GL_INFO_LOG_LENGTH)
    GL20.glGetProgramInfoLog(program, infoLogLength)
  }

  final def getRenderbufferParameteri(target: Int, pname: Int): Int = {
    GL30.glGetRenderbufferParameteri(target, pname)
  }

  final def getShaderParameteri(shader: Token.Shader, pname: Int): Int = {
    GL20.glGetShaderi(shader, pname)
  }

  final def getShaderParameterb(shader: Token.Shader, pname: Int): Boolean = {
    GL20.glGetShaderi(shader, pname) != GLES2.FALSE
  }

  final def getShaderPrecisionFormat(shadertype: Int, precisiontype: Int): PrecisionFormat = {
    this.tmpInt.clear()
    this.tmpInt.position(8)
    val tmpInt2 = tmpInt.slice() // No need to allocate a second buffer for this
    this.tmpInt.clear()
    this.tmpInt.limit(8)

    ARBES2Compatibility.glGetShaderPrecisionFormat(shadertype, precisiontype, tmpInt, tmpInt2)
    PrecisionFormat(tmpInt.get(0), tmpInt.get(1), tmpInt2.get(0))
  }

  final def getShaderInfoLog(shader: Token.Shader): String = {
    val infoLogLength = this.getShaderParameteri(shader, GL20.GL_INFO_LOG_LENGTH)
    GL20.glGetShaderInfoLog(shader, infoLogLength)
  }

  final def getShaderSource(shader: Token.Shader): String = {
    val sourceLength = this.getShaderParameteri(shader, GL20.GL_SHADER_SOURCE_LENGTH)
    GL20.glGetShaderSource(shader, sourceLength)
  }

  final def getTexParameteri(target: Int, pname: Int): Int = {
    GL11.glGetTexParameteri(target, pname)
  }

  final def getUniformi(program: Token.Program, location: Token.UniformLocation): Int = {
    this.tmpInt.clear()
    GL20.glGetUniform(program, location, this.tmpInt)
    this.tmpInt.get(0)
  }

  final def getUniformiv(program: Token.Program, location: Token.UniformLocation, outputs: IntBuffer): Unit = {
    GL20.glGetUniform(program, location, outputs)
  }

  final def getUniformf(program: Token.Program, location: Token.UniformLocation): Float = {
    this.tmpFloat.clear()
    GL20.glGetUniform(program, location, this.tmpFloat)
    this.tmpFloat.get(0)
  }

  final def getUniformfv(program: Token.Program, location: Token.UniformLocation, outputs: FloatBuffer): Unit = {
    GL20.glGetUniform(program, location, outputs)
  }

  final def getUniformLocation(program: Token.Program, name: String): Token.UniformLocation = {
    GL20.glGetUniformLocation(program, name)

  }

  final def getVertexAttribi(index: Int, pname: Int): Int = {
    this.tmpInt.clear()
    GL20.glGetVertexAttrib(index, pname, this.tmpInt)
    this.tmpInt.get(0)
  }

  final def getVertexAttribf(index: Int, pname: Int): Float = {
    this.tmpFloat.clear()
    GL20.glGetVertexAttrib(index, pname, this.tmpFloat)
    this.tmpFloat.get(0)
  }

  final def getVertexAttribfv(index: Int, pname: Int, outputs: FloatBuffer): Unit = {
    GL20.glGetVertexAttrib(index, pname, outputs)
  }

  final def getVertexAttribb(index: Int, pname: Int): Boolean = {
    this.getVertexAttribi(index, pname) != GLES2.FALSE
  }

  final def hint(target: Int, mode: Int): Unit = {
    GL11.glHint(target, mode)
  }

  final def isBuffer(buffer: Token.Buffer): Boolean = {
    GL15.glIsBuffer(buffer)
  }

  final def isEnabled(cap: Int): Boolean = {
    GL11.glIsEnabled(cap)
  }

  final def isFramebuffer(framebuffer: Token.FrameBuffer): Boolean = {
    GL30.glIsFramebuffer(framebuffer)
  }

  final def isProgram(program: Token.Program): Boolean = {
    GL20.glIsProgram(program)
  }

  final def isRenderbuffer(renderbuffer: Token.RenderBuffer): Boolean = {
    GL30.glIsRenderbuffer(renderbuffer)
  }

  final def isShader(shader: Token.Shader): Boolean = {
    GL20.glIsShader(shader)
  }

  final def isTexture(texture: Token.Texture): Boolean = {
    GL11.glIsTexture(texture)
  }

  final def lineWidth(width: Float): Unit = {
    GL11.glLineWidth(width)
  }

  final def linkProgram(program: Token.Program): Unit = {
    GL20.glLinkProgram(program)
  }

  final def pixelStorei(pname: Int, param: Int): Unit = {
    GL11.glPixelStorei(pname, param)
  }

  final def polygonOffset(factor: Float, units: Float): Unit = {
    GL11.glPolygonOffset(factor, units)
  }

  final def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: ByteBuffer): Unit = {
    GL11.glReadPixels(x, y, width, height, format, `type`, pixels)
  }
  final def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: ShortBuffer): Unit = {
    GL11.glReadPixels(x, y, width, height, format, `type`, pixels)
  }
  final def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: IntBuffer): Unit = {
    GL11.glReadPixels(x, y, width, height, format, `type`, pixels)
  }
  final def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: FloatBuffer): Unit = {
    GL11.glReadPixels(x, y, width, height, format, `type`, pixels)
  }
  final def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: DoubleBuffer): Unit = {
    GL11.glReadPixels(x, y, width, height, format, `type`, pixels)
  }

  final def renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int): Unit = {
    GL30.glRenderbufferStorage(target, internalformat, width, height)
  }

  final def sampleCoverage(value: Float, invert: Boolean): Unit = {
    GL13.glSampleCoverage(value, invert)
  }

  final def scissor(x: Int, y: Int, width: Int, height: Int): Unit = {
    GL11.glScissor(x, y, width, height)
  }

  final def shaderSource(shader: Token.Shader, source: String): Unit = {
    GL20.glShaderSource(shader, source)
  }

  final def stencilFunc(func: Int, ref: Int, mask: Int): Unit = {
    GL11.glStencilFunc(func, ref, mask)
  }

  final def stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int): Unit = {
    GL20.glStencilFuncSeparate(face, func, ref, mask)
  }

  final def stencilMask(mask: Int): Unit = {
    GL11.glStencilMask(mask)
  }

  final def stencilMaskSeparate(face: Int, mask: Int): Unit = {
    GL20.glStencilMaskSeparate(face, mask)
  }

  final def stencilOp(fail: Int, zfail: Int, zpass: Int): Unit = {
    GL11.glStencilOp(fail, zfail, zpass)
  }

  final def stencilOpSeparate(face: Int, sfail: Int, dpfail: Int, dppass: Int): Unit = {
    GL20.glStencilOpSeparate(face, sfail, dpfail, dppass)
  }

  final def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
    format: Int, `type`: Int, pixels: ByteBuffer): Unit = {
    GL11.glTexImage2D(target, level, internalformat, width, height, border, format, `type`, pixels)
  }
  final def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
    format: Int, `type`: Int, pixels: ShortBuffer): Unit = {
    GL11.glTexImage2D(target, level, internalformat, width, height, border, format, `type`, pixels)
  }
  final def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
    format: Int, `type`: Int, pixels: IntBuffer): Unit = {
    GL11.glTexImage2D(target, level, internalformat, width, height, border, format, `type`, pixels)
  }
  final def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
    format: Int, `type`: Int, pixels: FloatBuffer): Unit = {
    GL11.glTexImage2D(target, level, internalformat, width, height, border, format, `type`, pixels)
  }
  final def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
    format: Int, `type`: Int, pixels: DoubleBuffer): Unit = {
    GL11.glTexImage2D(target, level, internalformat, width, height, border, format, `type`, pixels)
  }
  final def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
    format: Int, `type`: Int): Unit = {
    GL11.glTexImage2D(target, level, internalformat, width, height, border, format, `type`, null: ByteBuffer)
  }

  final def texParameterf(target: Int, pname: Int, param: Float): Unit = {
    GL11.glTexParameterf(target, pname, param)
  }

  final def texParameteri(target: Int, pname: Int, param: Int): Unit = {
    GL11.glTexParameteri(target, pname, param)
  }

  final def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
    format: Int, `type`: Int, pixels: ByteBuffer): Unit = {
    GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, `type`, pixels)
  }
  final def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
    format: Int, `type`: Int, pixels: ShortBuffer): Unit = {
    GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, `type`, pixels)
  }
  final def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
    format: Int, `type`: Int, pixels: IntBuffer): Unit = {
    GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, `type`, pixels)
  }
  final def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
    format: Int, `type`: Int, pixels: FloatBuffer): Unit = {
    GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, `type`, pixels)
  }
  final def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
    format: Int, `type`: Int, pixels: DoubleBuffer): Unit = {
    GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, `type`, pixels)
  }

  final def uniform1f(location: Token.UniformLocation, x: Float): Unit = {
    GL20.glUniform1f(location, x)
  }

  final def uniform1fv(location: Token.UniformLocation, values: FloatBuffer): Unit = {
    GL20.glUniform1(location, values)
  }

  final def uniform1i(location: Token.UniformLocation, x: Int): Unit = {
    GL20.glUniform1i(location, x)
  }

  final def uniform1iv(location: Token.UniformLocation, values: IntBuffer): Unit = {
    GL20.glUniform1(location, values)
  }

  final def uniform2f(location: Token.UniformLocation, x: Float, y: Float): Unit = {
    GL20.glUniform2f(location, x, y)
  }

  final def uniform2fv(location: Token.UniformLocation, values: FloatBuffer): Unit = {
    GL20.glUniform2(location, values)
  }

  final def uniform2i(location: Token.UniformLocation, x: Int, y: Int): Unit = {
    GL20.glUniform2i(location, x, y)
  }

  final def uniform2iv(location: Token.UniformLocation, values: IntBuffer): Unit = {
    GL20.glUniform2(location, values)
  }

  final def uniform3f(location: Token.UniformLocation, x: Float, y: Float, z: Float): Unit = {
    GL20.glUniform3f(location, x, y, z)
  }

  final def uniform3fv(location: Token.UniformLocation, values: FloatBuffer): Unit = {
    GL20.glUniform3(location, values)
  }

  final def uniform3i(location: Token.UniformLocation, x: Int, y: Int, z: Int): Unit = {
    GL20.glUniform3i(location, x, y, z)
  }

  final def uniform3iv(location: Token.UniformLocation, values: IntBuffer): Unit = {
    GL20.glUniform3(location, values)
  }

  final def uniform4f(location: Token.UniformLocation, x: Float, y: Float, z: Float, w: Float): Unit = {
    GL20.glUniform4f(location, x, y, z, w)
  }

  final def uniform4fv(location: Token.UniformLocation, values: FloatBuffer): Unit = {
    GL20.glUniform4(location, values)
  }

  final def uniform4i(location: Token.UniformLocation, x: Int, y: Int, z: Int, w: Int): Unit = {
    GL20.glUniform4i(location, x, y, z, w)
  }

  final def uniform4iv(location: Token.UniformLocation, values: IntBuffer): Unit = {
    GL20.glUniform4(location, values)
  }

  final def uniformMatrix2fv(location: Token.UniformLocation, transpose: Boolean, matrices: FloatBuffer): Unit = {
    GL20.glUniformMatrix2(location, transpose, matrices)
  }

  final def uniformMatrix3fv(location: Token.UniformLocation, transpose: Boolean, matrices: FloatBuffer): Unit = {
    GL20.glUniformMatrix3(location, transpose, matrices)
  }

  final def uniformMatrix4fv(location: Token.UniformLocation, transpose: Boolean, matrices: FloatBuffer): Unit = {
    GL20.glUniformMatrix4(location, transpose, matrices)
  }

  final def useProgram(program: Token.Program): Unit = {
    GL20.glUseProgram(program)
  }

  final def validateProgram(program: Token.Program): Unit = {
    GL20.glValidateProgram(program)
  }

  final def vertexAttrib1f(index: Int, x: Float): Unit = {
    GL20.glVertexAttrib1f(index, x)
  }

  final def vertexAttrib1fv(index: Int, values: FloatBuffer): Unit = {
    val slice = values.slice
    GL20.glVertexAttrib1f(index, slice.get())
  }

  final def vertexAttrib2f(index: Int, x: Float, y: Float): Unit = {
    GL20.glVertexAttrib2f(index, x, y)
  }

  final def vertexAttrib2fv(index: Int, values: FloatBuffer): Unit = {
    val slice = values.slice
    GL20.glVertexAttrib2f(index, slice.get(), slice.get())
  }

  final def vertexAttrib3f(index: Int, x: Float, y: Float, z: Float): Unit = {
    GL20.glVertexAttrib3f(index, x, y, z)
  }

  final def vertexAttrib3fv(index: Int, values: FloatBuffer): Unit = {
    val slice = values.slice
    GL20.glVertexAttrib3f(index, slice.get(), slice.get(), slice.get())
  }

  final def vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float): Unit = {
    GL20.glVertexAttrib4f(index, x, y, z, w)
  }

  final def vertexAttrib4fv(index: Int, values: FloatBuffer): Unit = {
    val slice = values.slice
    GL20.glVertexAttrib4f(index, slice.get(), slice.get(), slice.get(), slice.get())
  }

  final def vertexAttribPointer(index: Int, size: Int, `type`: Int, normalized: Boolean, stride: Int, offset: Long): Unit = {
    GL20.glVertexAttribPointer(index, size, `type`, normalized, stride, offset)
  }

  final def viewport(x: Int, y: Int, width: Int, height: Int): Unit = {
    GL11.glViewport(x, y, width, height)
  }

  // Helper methods

  final def errorMessage(code: Int): String = {
    val msg: String = GLU.gluErrorString(code)
    msg
  }

  final def validProgram(program: Token.Program): Boolean = {
    (program > 0) && this.isProgram(program)
  }

  final def validShader(shader: Token.Shader): Boolean = {
    (shader > 0) && this.isShader(shader)
  }

  final def validBuffer(buffer: Token.Buffer): Boolean = {
    (buffer > 0) && this.isBuffer(buffer)
  }

  final def validUniformLocation(uloc: Token.UniformLocation): Boolean = {
    (uloc != -1)
  }

  final def validFramebuffer(fb: Token.FrameBuffer): Boolean = {
    (fb > 0) && this.isFramebuffer(fb)
  }

  final def validRenderbuffer(rb: Token.RenderBuffer): Boolean = {
    (rb > 0) && this.isRenderbuffer(rb)
  }

  final def differentPrograms(p1: Token.Program, p2: Token.Program): Boolean = {
    p1 != p2
  }
}

trait GLES2CompImpl extends GLES2CompRequirements {

  /* public API - constants */

  final val FALSE: Int = GL11.GL_FALSE
  final val TRUE: Int = GL11.GL_TRUE

  final val DEPTH_BUFFER_BIT: Int = GL11.GL_DEPTH_BUFFER_BIT
  final val STENCIL_BUFFER_BIT: Int = GL11.GL_STENCIL_BUFFER_BIT
  final val COLOR_BUFFER_BIT: Int = GL11.GL_COLOR_BUFFER_BIT
  final val POINTS: Int = GL11.GL_POINTS
  final val LINES: Int = GL11.GL_LINES
  final val LINE_LOOP: Int = GL11.GL_LINE_LOOP
  final val LINE_STRIP: Int = GL11.GL_LINE_STRIP
  final val TRIANGLES: Int = GL11.GL_TRIANGLES
  final val TRIANGLE_STRIP: Int = GL11.GL_TRIANGLE_STRIP
  final val TRIANGLE_FAN: Int = GL11.GL_TRIANGLE_FAN
  final val ZERO: Int = GL11.GL_ZERO
  final val ONE: Int = GL11.GL_ONE
  final val SRC_COLOR: Int = GL11.GL_SRC_COLOR
  final val ONE_MINUS_SRC_COLOR: Int = GL11.GL_ONE_MINUS_SRC_COLOR
  final val SRC_ALPHA: Int = GL11.GL_SRC_ALPHA
  final val ONE_MINUS_SRC_ALPHA: Int = GL11.GL_ONE_MINUS_SRC_ALPHA
  final val DST_ALPHA: Int = GL11.GL_DST_ALPHA
  final val ONE_MINUS_DST_ALPHA: Int = GL11.GL_ONE_MINUS_DST_ALPHA
  final val DST_COLOR: Int = GL11.GL_DST_COLOR
  final val ONE_MINUS_DST_COLOR: Int = GL11.GL_ONE_MINUS_DST_COLOR
  final val SRC_ALPHA_SATURATE: Int = GL11.GL_SRC_ALPHA_SATURATE
  final val FUNC_ADD: Int = GL14.GL_FUNC_ADD
  final val BLEND_EQUATION: Int = GL14.GL_BLEND_EQUATION
  final val BLEND_EQUATION_RGB: Int = GL20.GL_BLEND_EQUATION_RGB
  final val BLEND_EQUATION_ALPHA: Int = GL20.GL_BLEND_EQUATION_ALPHA
  final val FUNC_SUBTRACT: Int = GL14.GL_FUNC_SUBTRACT
  final val FUNC_REVERSE_SUBTRACT: Int = GL14.GL_FUNC_REVERSE_SUBTRACT
  final val BLEND_DST_RGB: Int = GL14.GL_BLEND_DST_RGB
  final val BLEND_SRC_RGB: Int = GL14.GL_BLEND_SRC_RGB
  final val BLEND_DST_ALPHA: Int = GL14.GL_BLEND_DST_ALPHA
  final val BLEND_SRC_ALPHA: Int = GL14.GL_BLEND_SRC_ALPHA
  final val CONSTANT_COLOR: Int = GL11.GL_CONSTANT_COLOR
  final val ONE_MINUS_CONSTANT_COLOR: Int = GL11.GL_ONE_MINUS_CONSTANT_COLOR
  final val CONSTANT_ALPHA: Int = GL11.GL_CONSTANT_ALPHA
  final val ONE_MINUS_CONSTANT_ALPHA: Int = GL11.GL_ONE_MINUS_CONSTANT_ALPHA
  final val BLEND_COLOR: Int = GL14.GL_BLEND_COLOR
  final val ARRAY_BUFFER: Int = GL15.GL_ARRAY_BUFFER
  final val ELEMENT_ARRAY_BUFFER: Int = GL15.GL_ELEMENT_ARRAY_BUFFER
  final val ARRAY_BUFFER_BINDING: Int = GL15.GL_ARRAY_BUFFER_BINDING
  final val ELEMENT_ARRAY_BUFFER_BINDING: Int = GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING
  final val STREAM_DRAW: Int = GL15.GL_STREAM_DRAW
  final val STATIC_DRAW: Int = GL15.GL_STATIC_DRAW
  final val DYNAMIC_DRAW: Int = GL15.GL_DYNAMIC_DRAW
  final val BUFFER_SIZE: Int = GL15.GL_BUFFER_SIZE
  final val BUFFER_USAGE: Int = GL15.GL_BUFFER_USAGE
  final val CURRENT_VERTEX_ATTRIB: Int = GL20.GL_CURRENT_VERTEX_ATTRIB
  final val FRONT: Int = GL11.GL_FRONT
  final val BACK: Int = GL11.GL_BACK
  final val FRONT_AND_BACK: Int = GL11.GL_FRONT_AND_BACK
  final val CULL_FACE: Int = GL11.GL_CULL_FACE
  final val BLEND: Int = GL11.GL_BLEND
  final val DITHER: Int = GL11.GL_DITHER
  final val STENCIL_TEST: Int = GL11.GL_STENCIL_TEST
  final val DEPTH_TEST: Int = GL11.GL_DEPTH_TEST
  final val SCISSOR_TEST: Int = GL11.GL_SCISSOR_TEST
  final val POLYGON_OFFSET_FILL: Int = GL11.GL_POLYGON_OFFSET_FILL
  final val SAMPLE_ALPHA_TO_COVERAGE: Int = GL13.GL_SAMPLE_ALPHA_TO_COVERAGE
  final val SAMPLE_COVERAGE: Int = GL13.GL_SAMPLE_COVERAGE
  final val NO_ERROR: Int = GL11.GL_NO_ERROR
  final val INVALID_ENUM: Int = GL11.GL_INVALID_ENUM
  final val INVALID_VALUE: Int = GL11.GL_INVALID_VALUE
  final val INVALID_OPERATION: Int = GL11.GL_INVALID_OPERATION
  final val OUT_OF_MEMORY: Int = GL11.GL_OUT_OF_MEMORY
  final val CW: Int = GL11.GL_CW
  final val CCW: Int = GL11.GL_CCW
  final val LINE_WIDTH: Int = GL11.GL_LINE_WIDTH
  final val ALIASED_POINT_SIZE_RANGE: Int = GL12.GL_ALIASED_POINT_SIZE_RANGE
  final val ALIASED_LINE_WIDTH_RANGE: Int = GL12.GL_ALIASED_LINE_WIDTH_RANGE
  final val CULL_FACE_MODE: Int = GL11.GL_CULL_FACE_MODE
  final val FRONT_FACE: Int = GL11.GL_FRONT_FACE
  final val DEPTH_RANGE: Int = GL11.GL_DEPTH_RANGE
  final val DEPTH_WRITEMASK: Int = GL11.GL_DEPTH_WRITEMASK
  final val DEPTH_CLEAR_VALUE: Int = GL11.GL_DEPTH_CLEAR_VALUE
  final val DEPTH_FUNC: Int = GL11.GL_DEPTH_FUNC
  final val STENCIL_CLEAR_VALUE: Int = GL11.GL_STENCIL_CLEAR_VALUE
  final val STENCIL_FUNC: Int = GL11.GL_STENCIL_FUNC
  final val STENCIL_FAIL: Int = GL11.GL_STENCIL_FAIL
  final val STENCIL_PASS_DEPTH_FAIL: Int = GL11.GL_STENCIL_PASS_DEPTH_FAIL
  final val STENCIL_PASS_DEPTH_PASS: Int = GL11.GL_STENCIL_PASS_DEPTH_PASS
  final val STENCIL_REF: Int = GL11.GL_STENCIL_REF
  final val STENCIL_VALUE_MASK: Int = GL11.GL_STENCIL_VALUE_MASK
  final val STENCIL_WRITEMASK: Int = GL11.GL_STENCIL_WRITEMASK
  final val STENCIL_BACK_FUNC: Int = GL20.GL_STENCIL_BACK_FUNC
  final val STENCIL_BACK_FAIL: Int = GL20.GL_STENCIL_BACK_FAIL
  final val STENCIL_BACK_PASS_DEPTH_FAIL: Int = GL20.GL_STENCIL_BACK_PASS_DEPTH_FAIL
  final val STENCIL_BACK_PASS_DEPTH_PASS: Int = GL20.GL_STENCIL_BACK_PASS_DEPTH_PASS
  final val STENCIL_BACK_REF: Int = GL20.GL_STENCIL_BACK_REF
  final val STENCIL_BACK_VALUE_MASK: Int = GL20.GL_STENCIL_BACK_VALUE_MASK
  final val STENCIL_BACK_WRITEMASK: Int = GL20.GL_STENCIL_BACK_WRITEMASK
  final val VIEWPORT: Int = GL11.GL_VIEWPORT
  final val SCISSOR_BOX: Int = GL11.GL_SCISSOR_BOX
  final val COLOR_CLEAR_VALUE: Int = GL11.GL_COLOR_CLEAR_VALUE
  final val COLOR_WRITEMASK: Int = GL11.GL_COLOR_WRITEMASK
  final val UNPACK_ALIGNMENT: Int = GL11.GL_UNPACK_ALIGNMENT
  final val PACK_ALIGNMENT: Int = GL11.GL_PACK_ALIGNMENT
  final val MAX_TEXTURE_SIZE: Int = GL11.GL_MAX_TEXTURE_SIZE
  final val MAX_VIEWPORT_DIMS: Int = GL11.GL_MAX_VIEWPORT_DIMS
  final val SUBPIXEL_BITS: Int = GL11.GL_SUBPIXEL_BITS
  final val RED_BITS: Int = GL11.GL_RED_BITS
  final val GREEN_BITS: Int = GL11.GL_GREEN_BITS
  final val BLUE_BITS: Int = GL11.GL_BLUE_BITS
  final val ALPHA_BITS: Int = GL11.GL_ALPHA_BITS
  final val DEPTH_BITS: Int = GL11.GL_DEPTH_BITS
  final val STENCIL_BITS: Int = GL11.GL_STENCIL_BITS
  final val POLYGON_OFFSET_UNITS: Int = GL11.GL_POLYGON_OFFSET_UNITS
  final val POLYGON_OFFSET_FACTOR: Int = GL11.GL_POLYGON_OFFSET_FACTOR
  final val TEXTURE_BINDING_2D: Int = GL11.GL_TEXTURE_BINDING_2D
  final val SAMPLE_BUFFERS: Int = GL13.GL_SAMPLE_BUFFERS
  final val SAMPLES: Int = GL13.GL_SAMPLES
  final val SAMPLE_COVERAGE_VALUE: Int = GL13.GL_SAMPLE_COVERAGE_VALUE
  final val SAMPLE_COVERAGE_INVERT: Int = GL13.GL_SAMPLE_COVERAGE_INVERT
  final val COMPRESSED_TEXTURE_FORMATS: Int = GL13.GL_COMPRESSED_TEXTURE_FORMATS
  final val DONT_CARE: Int = GL11.GL_DONT_CARE
  final val FASTEST: Int = GL11.GL_FASTEST
  final val NICEST: Int = GL11.GL_NICEST
  final val GENERATE_MIPMAP_HINT: Int = GL14.GL_GENERATE_MIPMAP_HINT
  final val BYTE: Int = GL11.GL_BYTE
  final val UNSIGNED_BYTE: Int = GL11.GL_UNSIGNED_BYTE
  final val SHORT: Int = GL11.GL_SHORT
  final val UNSIGNED_SHORT: Int = GL11.GL_UNSIGNED_SHORT
  final val INT: Int = GL11.GL_INT
  final val UNSIGNED_INT: Int = GL11.GL_UNSIGNED_INT
  final val FLOAT: Int = GL11.GL_FLOAT
  final val DEPTH_COMPONENT: Int = GL11.GL_DEPTH_COMPONENT
  final val ALPHA: Int = GL11.GL_ALPHA
  final val RGB: Int = GL11.GL_RGB
  final val RGBA: Int = GL11.GL_RGBA
  final val LUMINANCE: Int = GL11.GL_LUMINANCE
  final val LUMINANCE_ALPHA: Int = GL11.GL_LUMINANCE_ALPHA
  final val UNSIGNED_SHORT_4_4_4_4: Int = GL12.GL_UNSIGNED_SHORT_4_4_4_4
  final val UNSIGNED_SHORT_5_5_5_1: Int = GL12.GL_UNSIGNED_SHORT_5_5_5_1
  final val UNSIGNED_SHORT_5_6_5: Int = GL12.GL_UNSIGNED_SHORT_5_6_5
  final val FRAGMENT_SHADER: Int = GL20.GL_FRAGMENT_SHADER
  final val VERTEX_SHADER: Int = GL20.GL_VERTEX_SHADER
  final val MAX_VERTEX_ATTRIBS: Int = GL20.GL_MAX_VERTEX_ATTRIBS
  final val MAX_VERTEX_UNIFORM_VECTORS: Int = ARBES2Compatibility.GL_MAX_VERTEX_UNIFORM_VECTORS
  final val MAX_VARYING_VECTORS: Int = ARBES2Compatibility.GL_MAX_VARYING_VECTORS
  final val MAX_COMBINED_TEXTURE_IMAGE_UNITS: Int = GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS
  final val MAX_VERTEX_TEXTURE_IMAGE_UNITS: Int = GL20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS
  final val MAX_TEXTURE_IMAGE_UNITS: Int = GL20.GL_MAX_TEXTURE_IMAGE_UNITS
  final val MAX_FRAGMENT_UNIFORM_VECTORS: Int = ARBES2Compatibility.GL_MAX_FRAGMENT_UNIFORM_VECTORS
  final val SHADER_TYPE: Int = GL20.GL_SHADER_TYPE
  final val DELETE_STATUS: Int = GL20.GL_DELETE_STATUS
  final val LINK_STATUS: Int = GL20.GL_LINK_STATUS
  final val VALIDATE_STATUS: Int = GL20.GL_VALIDATE_STATUS
  final val ATTACHED_SHADERS: Int = GL20.GL_ATTACHED_SHADERS
  final val ACTIVE_UNIFORMS: Int = GL20.GL_ACTIVE_UNIFORMS
  final val ACTIVE_ATTRIBUTES: Int = GL20.GL_ACTIVE_ATTRIBUTES
  final val SHADING_LANGUAGE_VERSION: Int = GL20.GL_SHADING_LANGUAGE_VERSION
  final val CURRENT_PROGRAM: Int = GL20.GL_CURRENT_PROGRAM
  final val NEVER: Int = GL11.GL_NEVER
  final val LESS: Int = GL11.GL_LESS
  final val EQUAL: Int = GL11.GL_EQUAL
  final val LEQUAL: Int = GL11.GL_LEQUAL
  final val GREATER: Int = GL11.GL_GREATER
  final val NOTEQUAL: Int = GL11.GL_NOTEQUAL
  final val GEQUAL: Int = GL11.GL_GEQUAL
  final val ALWAYS: Int = GL11.GL_ALWAYS
  final val KEEP: Int = GL11.GL_KEEP
  final val REPLACE: Int = GL11.GL_REPLACE
  final val INCR: Int = GL11.GL_INCR
  final val DECR: Int = GL11.GL_DECR
  final val INVERT: Int = GL11.GL_INVERT
  final val INCR_WRAP: Int = GL14.GL_INCR_WRAP
  final val DECR_WRAP: Int = GL14.GL_DECR_WRAP
  final val VENDOR: Int = GL11.GL_VENDOR
  final val RENDERER: Int = GL11.GL_RENDERER
  final val VERSION: Int = GL11.GL_VERSION
  final val NEAREST: Int = GL11.GL_NEAREST
  final val LINEAR: Int = GL11.GL_LINEAR
  final val NEAREST_MIPMAP_NEAREST: Int = GL11.GL_NEAREST_MIPMAP_NEAREST
  final val LINEAR_MIPMAP_NEAREST: Int = GL11.GL_LINEAR_MIPMAP_NEAREST
  final val NEAREST_MIPMAP_LINEAR: Int = GL11.GL_NEAREST_MIPMAP_LINEAR
  final val LINEAR_MIPMAP_LINEAR: Int = GL11.GL_LINEAR_MIPMAP_LINEAR
  final val TEXTURE_MAG_FILTER: Int = GL11.GL_TEXTURE_MAG_FILTER
  final val TEXTURE_MIN_FILTER: Int = GL11.GL_TEXTURE_MIN_FILTER
  final val TEXTURE_WRAP_S: Int = GL11.GL_TEXTURE_WRAP_S
  final val TEXTURE_WRAP_T: Int = GL11.GL_TEXTURE_WRAP_T
  final val TEXTURE_2D: Int = GL11.GL_TEXTURE_2D
  final val TEXTURE: Int = GL11.GL_TEXTURE
  final val TEXTURE_CUBE_MAP: Int = GL13.GL_TEXTURE_CUBE_MAP
  final val TEXTURE_BINDING_CUBE_MAP: Int = GL13.GL_TEXTURE_BINDING_CUBE_MAP
  final val TEXTURE_CUBE_MAP_POSITIVE_X: Int = GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X
  final val TEXTURE_CUBE_MAP_NEGATIVE_X: Int = GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_X
  final val TEXTURE_CUBE_MAP_POSITIVE_Y: Int = GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_Y
  final val TEXTURE_CUBE_MAP_NEGATIVE_Y: Int = GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y
  final val TEXTURE_CUBE_MAP_POSITIVE_Z: Int = GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_Z
  final val TEXTURE_CUBE_MAP_NEGATIVE_Z: Int = GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z
  final val MAX_CUBE_MAP_TEXTURE_SIZE: Int = GL13.GL_MAX_CUBE_MAP_TEXTURE_SIZE
  final val TEXTURE0: Int = GL13.GL_TEXTURE0
  final val TEXTURE1: Int = GL13.GL_TEXTURE1
  final val TEXTURE2: Int = GL13.GL_TEXTURE2
  final val TEXTURE3: Int = GL13.GL_TEXTURE3
  final val TEXTURE4: Int = GL13.GL_TEXTURE4
  final val TEXTURE5: Int = GL13.GL_TEXTURE5
  final val TEXTURE6: Int = GL13.GL_TEXTURE6
  final val TEXTURE7: Int = GL13.GL_TEXTURE7
  final val TEXTURE8: Int = GL13.GL_TEXTURE8
  final val TEXTURE9: Int = GL13.GL_TEXTURE9
  final val TEXTURE10: Int = GL13.GL_TEXTURE10
  final val TEXTURE11: Int = GL13.GL_TEXTURE11
  final val TEXTURE12: Int = GL13.GL_TEXTURE12
  final val TEXTURE13: Int = GL13.GL_TEXTURE13
  final val TEXTURE14: Int = GL13.GL_TEXTURE14
  final val TEXTURE15: Int = GL13.GL_TEXTURE15
  final val TEXTURE16: Int = GL13.GL_TEXTURE16
  final val TEXTURE17: Int = GL13.GL_TEXTURE17
  final val TEXTURE18: Int = GL13.GL_TEXTURE18
  final val TEXTURE19: Int = GL13.GL_TEXTURE19
  final val TEXTURE20: Int = GL13.GL_TEXTURE20
  final val TEXTURE21: Int = GL13.GL_TEXTURE21
  final val TEXTURE22: Int = GL13.GL_TEXTURE22
  final val TEXTURE23: Int = GL13.GL_TEXTURE23
  final val TEXTURE24: Int = GL13.GL_TEXTURE24
  final val TEXTURE25: Int = GL13.GL_TEXTURE25
  final val TEXTURE26: Int = GL13.GL_TEXTURE26
  final val TEXTURE27: Int = GL13.GL_TEXTURE27
  final val TEXTURE28: Int = GL13.GL_TEXTURE28
  final val TEXTURE29: Int = GL13.GL_TEXTURE29
  final val TEXTURE30: Int = GL13.GL_TEXTURE30
  final val TEXTURE31: Int = GL13.GL_TEXTURE31
  final val ACTIVE_TEXTURE: Int = GL13.GL_ACTIVE_TEXTURE
  final val REPEAT: Int = GL11.GL_REPEAT
  final val CLAMP_TO_EDGE: Int = GL12.GL_CLAMP_TO_EDGE
  final val MIRRORED_REPEAT: Int = GL14.GL_MIRRORED_REPEAT
  final val FLOAT_VEC2: Int = GL20.GL_FLOAT_VEC2
  final val FLOAT_VEC3: Int = GL20.GL_FLOAT_VEC3
  final val FLOAT_VEC4: Int = GL20.GL_FLOAT_VEC4
  final val INT_VEC2: Int = GL20.GL_INT_VEC2
  final val INT_VEC3: Int = GL20.GL_INT_VEC3
  final val INT_VEC4: Int = GL20.GL_INT_VEC4
  final val BOOL: Int = GL20.GL_BOOL
  final val BOOL_VEC2: Int = GL20.GL_BOOL_VEC2
  final val BOOL_VEC3: Int = GL20.GL_BOOL_VEC3
  final val BOOL_VEC4: Int = GL20.GL_BOOL_VEC4
  final val FLOAT_MAT2: Int = GL20.GL_FLOAT_MAT2
  final val FLOAT_MAT3: Int = GL20.GL_FLOAT_MAT3
  final val FLOAT_MAT4: Int = GL20.GL_FLOAT_MAT4
  final val SAMPLER_2D: Int = GL20.GL_SAMPLER_2D
  final val SAMPLER_CUBE: Int = GL20.GL_SAMPLER_CUBE
  final val VERTEX_ATTRIB_ARRAY_ENABLED: Int = GL20.GL_VERTEX_ATTRIB_ARRAY_ENABLED
  final val VERTEX_ATTRIB_ARRAY_SIZE: Int = GL20.GL_VERTEX_ATTRIB_ARRAY_SIZE
  final val VERTEX_ATTRIB_ARRAY_STRIDE: Int = GL20.GL_VERTEX_ATTRIB_ARRAY_STRIDE
  final val VERTEX_ATTRIB_ARRAY_TYPE: Int = GL20.GL_VERTEX_ATTRIB_ARRAY_TYPE
  final val VERTEX_ATTRIB_ARRAY_NORMALIZED: Int = GL20.GL_VERTEX_ATTRIB_ARRAY_NORMALIZED
  final val VERTEX_ATTRIB_ARRAY_POINTER: Int = GL20.GL_VERTEX_ATTRIB_ARRAY_POINTER
  final val VERTEX_ATTRIB_ARRAY_BUFFER_BINDING: Int = GL15.GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING
  final val COMPILE_STATUS: Int = GL20.GL_COMPILE_STATUS
  final val LOW_FLOAT: Int = ARBES2Compatibility.GL_LOW_FLOAT
  final val MEDIUM_FLOAT: Int = ARBES2Compatibility.GL_MEDIUM_FLOAT
  final val HIGH_FLOAT: Int = ARBES2Compatibility.GL_HIGH_FLOAT
  final val LOW_INT: Int = ARBES2Compatibility.GL_LOW_INT
  final val MEDIUM_INT: Int = ARBES2Compatibility.GL_MEDIUM_INT
  final val HIGH_INT: Int = ARBES2Compatibility.GL_HIGH_INT
  final val FRAMEBUFFER: Int = GL30.GL_FRAMEBUFFER
  final val RENDERBUFFER: Int = GL30.GL_RENDERBUFFER
  final val RGBA4: Int = GL11.GL_RGBA4
  final val RGB5_A1: Int = GL11.GL_RGB5_A1
  final val RGB565: Int = ARBES2Compatibility.GL_RGB565
  final val DEPTH_COMPONENT16: Int = GL14.GL_DEPTH_COMPONENT16
  final val STENCIL_INDEX: Int = GL11.GL_STENCIL_INDEX
  final val STENCIL_INDEX8: Int = GL30.GL_STENCIL_INDEX8
  final val DEPTH_STENCIL: Int = GL30.GL_DEPTH_STENCIL
  final val RENDERBUFFER_WIDTH: Int = GL30.GL_RENDERBUFFER_WIDTH
  final val RENDERBUFFER_HEIGHT: Int = GL30.GL_RENDERBUFFER_HEIGHT
  final val RENDERBUFFER_INTERNAL_FORMAT: Int = GL30.GL_RENDERBUFFER_INTERNAL_FORMAT
  final val RENDERBUFFER_RED_SIZE: Int = GL30.GL_RENDERBUFFER_RED_SIZE
  final val RENDERBUFFER_GREEN_SIZE: Int = GL30.GL_RENDERBUFFER_GREEN_SIZE
  final val RENDERBUFFER_BLUE_SIZE: Int = GL30.GL_RENDERBUFFER_BLUE_SIZE
  final val RENDERBUFFER_ALPHA_SIZE: Int = GL30.GL_RENDERBUFFER_ALPHA_SIZE
  final val RENDERBUFFER_DEPTH_SIZE: Int = GL30.GL_RENDERBUFFER_DEPTH_SIZE
  final val RENDERBUFFER_STENCIL_SIZE: Int = GL30.GL_RENDERBUFFER_STENCIL_SIZE
  final val FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE: Int = GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
  final val FRAMEBUFFER_ATTACHMENT_OBJECT_NAME: Int = GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME
  final val FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL: Int = GL30.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL
  final val FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE: Int = GL30.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE
  final val COLOR_ATTACHMENT0: Int = GL30.GL_COLOR_ATTACHMENT0
  final val DEPTH_ATTACHMENT: Int = GL30.GL_DEPTH_ATTACHMENT
  final val STENCIL_ATTACHMENT: Int = GL30.GL_STENCIL_ATTACHMENT
  final val DEPTH_STENCIL_ATTACHMENT: Int = GL30.GL_DEPTH_STENCIL_ATTACHMENT
  final val NONE: Int = GL11.GL_NONE
  final val FRAMEBUFFER_COMPLETE: Int = GL30.GL_FRAMEBUFFER_COMPLETE
  final val FRAMEBUFFER_INCOMPLETE_ATTACHMENT: Int = GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT
  final val FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT: Int = GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT
  final val FRAMEBUFFER_INCOMPLETE_DIMENSIONS: Int = EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT // no present in standard OpenGL 3.0
  // GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER not present in WebGL & Android, moved to Macroglex
  // GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER not present in WebGL & Android, moved to Macroglex
  final val FRAMEBUFFER_UNSUPPORTED: Int = GL30.GL_FRAMEBUFFER_UNSUPPORTED
  final val FRAMEBUFFER_BINDING: Int = GL30.GL_FRAMEBUFFER_BINDING
  final val RENDERBUFFER_BINDING: Int = GL30.GL_RENDERBUFFER_BINDING
  final val MAX_RENDERBUFFER_SIZE: Int = GL30.GL_MAX_RENDERBUFFER_SIZE
  final val INVALID_FRAMEBUFFER_OPERATION: Int = GL30.GL_INVALID_FRAMEBUFFER_OPERATION

  /* public API - methods */

  final def createByteData(sz: Int): ByteBuffer = {
    org.lwjgl.BufferUtils.createByteBuffer(sz)
  }

  final def createShortData(sz: Int): ShortBuffer = {
    org.lwjgl.BufferUtils.createShortBuffer(sz)
  }

  final def createIntData(sz: Int): IntBuffer = {
    org.lwjgl.BufferUtils.createIntBuffer(sz)
  }

  final def createFloatData(sz: Int): FloatBuffer = {
    org.lwjgl.BufferUtils.createFloatBuffer(sz)
  }

  final def createDoubleData(sz: Int): DoubleBuffer = {
    org.lwjgl.BufferUtils.createDoubleBuffer(sz)
  }

  /* public API - implicits */

  implicit lazy val defaultGLES2 = new GLES2LWJGL()

  /* implementation-specific methods */

}

