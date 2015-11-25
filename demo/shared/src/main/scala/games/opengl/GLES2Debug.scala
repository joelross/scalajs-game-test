package games.opengl

import java.nio.{ ByteBuffer, ShortBuffer, IntBuffer, FloatBuffer }

class GLES2Debug(ogl: GLES2) extends GLES2 {

  /* Debug specifics */

  final def getInternalContext(): GLES2 = ogl

  // Unchecked methods

  final def getError(): Int = {
    ogl.getError()
  }

  final def errorMessage(code: Int): String = {
    ogl.errorMessage(code)
  }

  final def differentPrograms(p1: Token.Program, p2: Token.Program): Boolean = {
    ogl.differentPrograms(p1, p2)
  }

  final def display: Display = ogl.display

  // Checked methods

  final def activeTexture(texture: Int): Unit = {
    ogl.activeTexture(texture)
    this.checkError()
  }

  final def attachShader(program: Token.Program, shader: Token.Shader): Unit = {
    ogl.attachShader(program, shader)
    this.checkError()
  }

  final def bindAttribLocation(program: Token.Program, index: Int, name: String): Unit = {
    ogl.bindAttribLocation(program, index, name)
    this.checkError()
  }

  final def bindBuffer(target: Int, buffer: Token.Buffer): Unit = {
    ogl.bindBuffer(target, buffer)
    this.checkError()
  }

  final def bindFramebuffer(target: Int, framebuffer: Token.FrameBuffer): Unit = {
    ogl.bindFramebuffer(target, framebuffer)
    this.checkError()
  }

  final def bindRenderbuffer(target: Int, renderbuffer: Token.RenderBuffer): Unit = {
    ogl.bindRenderbuffer(target, renderbuffer)
    this.checkError()
  }

  final def bindTexture(target: Int, texture: Token.Texture): Unit = {
    ogl.bindTexture(target, texture)
    this.checkError()
  }

  final def blendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = {
    ogl.blendColor(red, green, blue, alpha)
    this.checkError()
  }

  final def blendEquation(mode: Int): Unit = {
    ogl.blendEquation(mode)
    this.checkError()
  }

  final def blendEquationSeparate(modeRGB: Int, modeAlpha: Int): Unit = {
    ogl.blendEquationSeparate(modeRGB, modeAlpha)
    this.checkError()
  }

  final def blendFunc(sfactor: Int, dfactor: Int): Unit = {
    ogl.blendFunc(sfactor, dfactor)
    this.checkError()
  }

  final def blendFuncSeparate(srcfactorRGB: Int, dstfactorRGB: Int, srcfactorAlpha: Int, dstfactorAlpha: Int): Unit = {
    ogl.blendFuncSeparate(srcfactorRGB, dstfactorRGB, srcfactorAlpha, dstfactorAlpha)
    this.checkError()
  }

  final def bufferData(target: Int, totalBytes: Long, usage: Int): Unit = {
    ogl.bufferData(target, totalBytes, usage)
    this.checkError()
  }

  final def bufferData(target: Int, data: ByteBuffer, usage: Int): Unit = {
    ogl.bufferData(target, data, usage)
    this.checkError()
  }
  final def bufferData(target: Int, data: ShortBuffer, usage: Int): Unit = {
    ogl.bufferData(target, data, usage)
    this.checkError()
  }
  final def bufferData(target: Int, data: IntBuffer, usage: Int): Unit = {
    ogl.bufferData(target, data, usage)
    this.checkError()
  }
  final def bufferData(target: Int, data: FloatBuffer, usage: Int): Unit = {
    ogl.bufferData(target, data, usage)
    this.checkError()
  }

  final def bufferSubData(target: Int, offset: Long, data: ByteBuffer): Unit = {
    ogl.bufferSubData(target, offset, data)
    this.checkError()
  }
  final def bufferSubData(target: Int, offset: Long, data: ShortBuffer): Unit = {
    ogl.bufferSubData(target, offset, data)
    this.checkError()
  }
  final def bufferSubData(target: Int, offset: Long, data: IntBuffer): Unit = {
    ogl.bufferSubData(target, offset, data)
    this.checkError()
  }
  final def bufferSubData(target: Int, offset: Long, data: FloatBuffer): Unit = {
    ogl.bufferSubData(target, offset, data)
    this.checkError()
  }

  final def checkFramebufferStatus(target: Int): Int = {
    val ret = ogl.checkFramebufferStatus(target)
    this.checkError()
    ret
  }

  final def clear(mask: Int): Unit = {
    ogl.clear(mask)
    this.checkError()
  }

  final def clearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = {
    ogl.clearColor(red, green, blue, alpha)
    this.checkError()
  }

  final def clearDepth(depth: Float): Unit = {
    ogl.clearDepth(depth)
    this.checkError()
  }

  final def clearStencil(s: Int): Unit = {
    ogl.clearStencil(s)
    this.checkError()
  }

  final def colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit = {
    ogl.colorMask(red, green, blue, alpha)
    this.checkError()
  }

  final def compileShader(shader: Token.Shader): Unit = {
    ogl.compileShader(shader)
    this.checkError()

    // check for errors in the compilation
    if (ogl.getShaderParameterb(shader, GLES2.COMPILE_STATUS) == false) {
      val msg = ogl.getShaderInfoLog(shader)
      throw new GLException("Error in the compilation of the shader : " + msg)
    }
  }

  final def compressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                                 data: ByteBuffer): Unit = {
    ogl.compressedTexImage2D(target, level, internalformat, width, height, border, data)
    this.checkError()
  }

  final def compressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                                    format: Int, data: ByteBuffer): Unit = {
    ogl.compressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, data)
    this.checkError()
  }

  final def copyTexImage2D(target: Int, level: Int, internalFormat: Int, x: Int, y: Int, width: Int, height: Int, border: Int): Unit = {
    ogl.copyTexImage2D(target, level, internalFormat, x, y, width, height, border)
    this.checkError()
  }

  final def copyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit = {
    ogl.copyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)
    this.checkError()
  }

  final def createBuffer(): Token.Buffer = {
    val ret = ogl.createBuffer
    this.checkError()
    ret
  }

  final def createFramebuffer(): Token.FrameBuffer = {
    val ret = ogl.createFramebuffer()
    this.checkError()
    ret
  }

  final def createProgram(): Token.Program = {
    val ret = ogl.createProgram()
    this.checkError()
    ret
  }

  final def createRenderbuffer(): Token.RenderBuffer = {
    val ret = ogl.createRenderbuffer()
    this.checkError()
    ret
  }

  final def createShader(`type`: Int): Token.Shader = {
    val ret = ogl.createShader(`type`)
    this.checkError()
    ret
  }

  final def createTexture(): Token.Texture = {
    val ret = ogl.createTexture()
    this.checkError()
    ret
  }

  final def cullFace(mode: Int): Unit = {
    ogl.cullFace(mode)
    this.checkError()
  }

  final def deleteBuffer(buffer: Token.Buffer): Unit = {
    ogl.deleteBuffer(buffer)
    this.checkError()
  }

  final def deleteFramebuffer(framebuffer: Token.FrameBuffer): Unit = {
    ogl.deleteFramebuffer(framebuffer)
    this.checkError()
  }

  final def deleteProgram(program: Token.Program): Unit = {
    ogl.deleteProgram(program)
    this.checkError()
  }

  final def deleteRenderbuffer(renderbuffer: Token.RenderBuffer): Unit = {
    ogl.deleteRenderbuffer(renderbuffer)
    this.checkError()
  }

  final def deleteShader(shader: Token.Shader): Unit = {
    ogl.deleteShader(shader)
    this.checkError()
  }

  final def deleteTexture(texture: Token.Texture): Unit = {
    ogl.deleteTexture(texture)
    this.checkError()
  }

  final def depthFunc(func: Int): Unit = {
    ogl.depthFunc(func)
    this.checkError()
  }

  final def depthMask(flag: Boolean): Unit = {
    ogl.depthMask(flag)
    this.checkError()
  }

  final def depthRange(zNear: Float, zFar: Float): Unit = {
    ogl.depthRange(zNear, zFar)
    this.checkError()
  }

  final def detachShader(program: Token.Program, shader: Token.Shader): Unit = {
    ogl.detachShader(program, shader)
    this.checkError()
  }

  final def disable(cap: Int): Unit = {
    ogl.disable(cap)
    this.checkError()
  }

  final def disableVertexAttribArray(index: Int): Unit = {
    ogl.disableVertexAttribArray(index)
    this.checkError()
  }

  final def drawArrays(mode: Int, first: Int, count: Int): Unit = {
    ogl.drawArrays(mode, first, count)
    this.checkError()
  }

  final def drawElements(mode: Int, count: Int, `type`: Int, offset: Long): Unit = {
    ogl.drawElements(mode, count, `type`, offset)
    this.checkError()
  }
  final def drawElements(mode: Int, count: Int, `type`: Int, offset: Int): Unit = {
    ogl.drawElements(mode, count, `type`, offset)
    this.checkError()
  }

  final def enable(cap: Int): Unit = {
    ogl.enable(cap)
    this.checkError()
  }

  final def enableVertexAttribArray(index: Int): Unit = {
    ogl.enableVertexAttribArray(index)
    this.checkError()
  }

  final def finish(): Unit = {
    ogl.finish()
    this.checkError()
  }

  final def flush(): Unit = {
    ogl.flush()
    this.checkError()
  }

  final def framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Token.RenderBuffer): Unit = {
    ogl.framebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)
    this.checkError()
  }

  final def framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Token.Texture, level: Int): Unit = {
    ogl.framebufferTexture2D(target, attachment, textarget, texture, level)
    this.checkError()
  }

  final def frontFace(mode: Int): Unit = {
    ogl.frontFace(mode)
    this.checkError()
  }

  final def generateMipmap(target: Int): Unit = {
    ogl.generateMipmap(target)
    this.checkError()
  }

  final def getActiveAttrib(program: Token.Program, index: Int): ActiveInfo = {
    val ret = ogl.getActiveAttrib(program, index)
    this.checkError()
    ret
  }

  final def getActiveUniform(program: Token.Program, index: Int): ActiveInfo = {
    val ret = ogl.getActiveUniform(program, index)
    this.checkError()
    ret
  }

  final def getAttachedShaders(program: Token.Program): Array[Token.Shader] = {
    val ret = ogl.getAttachedShaders(program)
    this.checkError()
    ret
  }

  final def getAttribLocation(program: Token.Program, name: String): Int = {
    val ret = ogl.getAttribLocation(program, name)
    this.checkError()
    ret
  }

  final def getBufferParameteri(target: Int, pname: Int): Int = {
    val ret = ogl.getBufferParameteri(target, pname)
    this.checkError()
    ret
  }

  final def getParameterBuffer(pname: Int): Token.Buffer = {
    val ret = ogl.getParameterBuffer(pname)
    this.checkError()
    ret
  }

  final def getParameterTexture(pname: Int): Token.Texture = {
    val ret = ogl.getParameterTexture(pname)
    this.checkError()
    ret
  }

  final def getParameterFramebuffer(pname: Int): Token.FrameBuffer = {
    val ret = ogl.getParameterFramebuffer(pname)
    this.checkError()
    ret
  }

  final def getParameterProgram(pname: Int): Token.Program = {
    val ret = ogl.getParameterProgram(pname)
    this.checkError()
    ret
  }

  final def getParameterRenderbuffer(pname: Int): Token.RenderBuffer = {
    val ret = ogl.getParameterRenderbuffer(pname)
    this.checkError()
    ret
  }

  final def getParameterShader(pname: Int): Token.Shader = {
    val ret = ogl.getParameterShader(pname)
    this.checkError()
    ret
  }

  final def getParameterString(pname: Int): String = {
    val ret = ogl.getParameterString(pname)
    this.checkError()
    ret
  }

  final def getParameteri(pname: Int): Int = {
    val ret = ogl.getParameteri(pname)
    this.checkError()
    ret
  }

  final def getParameteriv(pname: Int, outputs: IntBuffer): Unit = {
    ogl.getParameteriv(pname, outputs)
    this.checkError()
  }

  final def getParameterf(pname: Int): Float = {
    val ret = ogl.getParameterf(pname)
    this.checkError()
    ret
  }

  final def getParameterfv(pname: Int, outputs: FloatBuffer): Unit = {
    ogl.getParameterfv(pname, outputs)
    this.checkError()
  }

  final def getParameterb(pname: Int): Boolean = {
    val ret = ogl.getParameterb(pname)
    this.checkError()
    ret
  }

  final def getParameterbv(pname: Int, outputs: ByteBuffer): Unit = {
    ogl.getParameterbv(pname, outputs)
    this.checkError()
  }

  final def getFramebufferAttachmentParameteri(target: Int, attachment: Int, pname: Int): Int = {
    val ret = ogl.getFramebufferAttachmentParameteri(target, attachment, pname)
    this.checkError()
    ret
  }

  final def getFramebufferAttachmentParameterRenderbuffer(target: Int, attachment: Int, pname: Int): Token.RenderBuffer = {
    val ret = ogl.getFramebufferAttachmentParameterRenderbuffer(target, attachment, pname)
    this.checkError()
    ret
  }

  final def getFramebufferAttachmentParameterTexture(target: Int, attachment: Int, pname: Int): Token.Texture = {
    val ret = ogl.getFramebufferAttachmentParameterTexture(target, attachment, pname)
    this.checkError()
    ret
  }

  final def getProgramParameteri(program: Token.Program, pname: Int): Int = {
    val ret = ogl.getProgramParameteri(program, pname)
    this.checkError()
    ret
  }

  final def getProgramParameterb(program: Token.Program, pname: Int): Boolean = {
    val ret = ogl.getProgramParameterb(program, pname)
    this.checkError()
    ret
  }

  final def getProgramInfoLog(program: Token.Program): String = {
    val ret = ogl.getProgramInfoLog(program)
    this.checkError()
    ret
  }

  final def getRenderbufferParameteri(target: Int, pname: Int): Int = {
    val ret = ogl.getRenderbufferParameteri(target, pname)
    this.checkError()
    ret
  }

  final def getShaderParameteri(shader: Token.Shader, pname: Int): Int = {
    val ret = ogl.getShaderParameteri(shader, pname)
    this.checkError()
    ret
  }

  final def getShaderParameterb(shader: Token.Shader, pname: Int): Boolean = {
    val ret = ogl.getShaderParameterb(shader, pname)
    this.checkError()
    ret
  }

  final def getShaderPrecisionFormat(shadertype: Int, precisiontype: Int): PrecisionFormat = {
    val ret = ogl.getShaderPrecisionFormat(shadertype, precisiontype)
    this.checkError()
    ret
  }

  final def getShaderInfoLog(shader: Token.Shader): String = {
    val ret = ogl.getShaderInfoLog(shader)
    this.checkError()
    ret
  }

  final def getShaderSource(shader: Token.Shader): String = {
    val ret = ogl.getShaderSource(shader)
    this.checkError()
    ret
  }

  final def getTexParameteri(target: Int, pname: Int): Int = {
    val ret = ogl.getTexParameteri(target, pname)
    this.checkError()
    ret
  }

  final def getUniformi(program: Token.Program, location: Token.UniformLocation): Int = {
    val ret = ogl.getUniformi(program, location)
    this.checkError()
    ret
  }

  final def getUniformiv(program: Token.Program, location: Token.UniformLocation, outputs: IntBuffer): Unit = {
    val ret = ogl.getUniformiv(program, location, outputs)
    this.checkError()
    ret
  }

  final def getUniformf(program: Token.Program, location: Token.UniformLocation): Float = {
    val ret = ogl.getUniformf(program, location)
    this.checkError()
    ret
  }

  final def getUniformfv(program: Token.Program, location: Token.UniformLocation, outputs: FloatBuffer): Unit = {
    ogl.getUniformfv(program, location, outputs)
    this.checkError()
  }

  final def getUniformLocation(program: Token.Program, name: String): Token.UniformLocation = {
    val ret = ogl.getUniformLocation(program, name)
    this.checkError()
    ret
  }

  final def getVertexAttribi(index: Int, pname: Int): Int = {
    val ret = ogl.getVertexAttribi(index, pname)
    this.checkError()
    ret
  }

  final def getVertexAttribiv(index: Int, pname: Int, outputs: IntBuffer): Unit = {
    val ret = ogl.getVertexAttribiv(index, pname, outputs)
    this.checkError()
    ret
  }

  final def getVertexAttribf(index: Int, pname: Int): Float = {
    val ret = ogl.getVertexAttribf(index, pname)
    this.checkError()
    ret
  }

  final def getVertexAttribfv(index: Int, pname: Int, outputs: FloatBuffer): Unit = {
    val ret = ogl.getVertexAttribfv(index, pname, outputs)
    this.checkError()
    ret
  }

  final def getVertexAttribb(index: Int, pname: Int): Boolean = {
    val ret = ogl.getVertexAttribb(index, pname)
    this.checkError()
    ret
  }

  final def hint(target: Int, mode: Int): Unit = {
    ogl.hint(target, mode)
    this.checkError()
  }

  final def isBuffer(buffer: Token.Buffer): Boolean = {
    val ret = ogl.isBuffer(buffer)
    this.checkError()
    ret
  }

  final def isEnabled(cap: Int): Boolean = {
    val ret = ogl.isEnabled(cap)
    this.checkError()
    ret
  }

  final def isFramebuffer(framebuffer: Token.FrameBuffer): Boolean = {
    val ret = ogl.isFramebuffer(framebuffer)
    this.checkError()
    ret
  }

  final def isProgram(program: Token.Program): Boolean = {
    val ret = ogl.isProgram(program)
    this.checkError()
    ret
  }

  final def isRenderbuffer(renderbuffer: Token.RenderBuffer): Boolean = {
    val ret = ogl.isRenderbuffer(renderbuffer)
    this.checkError()
    ret
  }

  final def isShader(shader: Token.Shader): Boolean = {
    val ret = ogl.isShader(shader)
    this.checkError()
    ret
  }

  final def isTexture(texture: Token.Texture): Boolean = {
    val ret = ogl.isTexture(texture)
    this.checkError()
    ret
  }

  final def lineWidth(width: Float): Unit = {
    ogl.lineWidth(width)
    this.checkError()
  }

  final def linkProgram(program: Token.Program): Unit = {
    ogl.linkProgram(program)
    this.checkError()

    // check for errors in the linking
    if (ogl.getProgramParameterb(program, GLES2.LINK_STATUS) == false) {
      val msg = ogl.getProgramInfoLog(program)
      throw new GLException("Error in the linking of the program : " + msg)
    }
  }

  final def pixelStorei(pname: Int, param: Int): Unit = {
    ogl.pixelStorei(pname, param)
    this.checkError()
  }

  final def polygonOffset(factor: Float, units: Float): Unit = {
    ogl.polygonOffset(factor, units)
    this.checkError()
  }

  final def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: ByteBuffer): Unit = {
    ogl.readPixels(x, y, width, height, format, `type`, pixels)
    this.checkError()
  }
  final def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: ShortBuffer): Unit = {
    ogl.readPixels(x, y, width, height, format, `type`, pixels)
    this.checkError()
  }
  final def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: IntBuffer): Unit = {
    ogl.readPixels(x, y, width, height, format, `type`, pixels)
    this.checkError()
  }
  final def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: FloatBuffer): Unit = {
    ogl.readPixels(x, y, width, height, format, `type`, pixels)
    this.checkError()
  }

  final def renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int): Unit = {
    ogl.renderbufferStorage(target, internalformat, width, height)
    this.checkError()
  }

  final def sampleCoverage(value: Float, invert: Boolean): Unit = {
    ogl.sampleCoverage(value, invert)
    this.checkError()
  }

  final def scissor(x: Int, y: Int, width: Int, height: Int): Unit = {
    ogl.scissor(x, y, width, height)
    this.checkError()
  }

  final def shaderSource(shader: Token.Shader, source: String): Unit = {
    ogl.shaderSource(shader, source)
    this.checkError()
  }

  final def stencilFunc(func: Int, ref: Int, mask: Int): Unit = {
    ogl.stencilFunc(func, ref, mask)
    this.checkError()
  }

  final def stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int): Unit = {
    ogl.stencilFuncSeparate(face, func, ref, mask)
    this.checkError()
  }

  final def stencilMask(mask: Int): Unit = {
    ogl.stencilMask(mask)
    this.checkError()
  }

  final def stencilMaskSeparate(face: Int, mask: Int): Unit = {
    ogl.stencilMaskSeparate(face, mask)
    this.checkError()
  }

  final def stencilOp(fail: Int, zfail: Int, zpass: Int): Unit = {
    ogl.stencilOp(fail, zfail, zpass)
    this.checkError()
  }

  final def stencilOpSeparate(face: Int, sfail: Int, dpfail: Int, dppass: Int): Unit = {
    ogl.stencilOpSeparate(face, sfail, dpfail, dppass)
    this.checkError()
  }

  final def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                       format: Int, `type`: Int, pixels: ByteBuffer): Unit = {
    ogl.texImage2D(target, level, internalformat, width, height, border, format, `type`, pixels)
    this.checkError()
  }
  final def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                       format: Int, `type`: Int, pixels: ShortBuffer): Unit = {
    ogl.texImage2D(target, level, internalformat, width, height, border, format, `type`, pixels)
    this.checkError()
  }
  final def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                       format: Int, `type`: Int, pixels: IntBuffer): Unit = {
    ogl.texImage2D(target, level, internalformat, width, height, border, format, `type`, pixels)
    this.checkError()
  }
  final def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                       format: Int, `type`: Int, pixels: FloatBuffer): Unit = {
    ogl.texImage2D(target, level, internalformat, width, height, border, format, `type`, pixels)
    this.checkError()
  }
  final def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                       format: Int, `type`: Int): Unit = {
    ogl.texImage2D(target, level, internalformat, width, height, border, format, `type`)
    this.checkError()
  }

  final def texParameterf(target: Int, pname: Int, param: Float): Unit = {
    ogl.texParameterf(target, pname, param)
    this.checkError()
  }

  final def texParameteri(target: Int, pname: Int, param: Int): Unit = {
    ogl.texParameteri(target, pname, param)
    this.checkError()
  }

  final def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                          format: Int, `type`: Int, pixels: ByteBuffer): Unit = {
    ogl.texSubImage2D(target, level, xoffset, yoffset, width, height, format, `type`, pixels)
    this.checkError()
  }
  final def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                          format: Int, `type`: Int, pixels: ShortBuffer): Unit = {
    ogl.texSubImage2D(target, level, xoffset, yoffset, width, height, format, `type`, pixels)
    this.checkError()
  }
  final def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                          format: Int, `type`: Int, pixels: IntBuffer): Unit = {
    ogl.texSubImage2D(target, level, xoffset, yoffset, width, height, format, `type`, pixels)
    this.checkError()
  }
  final def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                          format: Int, `type`: Int, pixels: FloatBuffer): Unit = {
    ogl.texSubImage2D(target, level, xoffset, yoffset, width, height, format, `type`, pixels)
    this.checkError()
  }

  final def uniform1f(location: Token.UniformLocation, x: Float): Unit = {
    ogl.uniform1f(location, x)
    this.checkError()
  }

  final def uniform1fv(location: Token.UniformLocation, values: FloatBuffer): Unit = {
    ogl.uniform1fv(location, values)
    this.checkError()
  }

  final def uniform1i(location: Token.UniformLocation, x: Int): Unit = {
    ogl.uniform1i(location, x)
    this.checkError()
  }

  final def uniform1iv(location: Token.UniformLocation, values: IntBuffer): Unit = {
    ogl.uniform1iv(location, values)
    this.checkError()
  }

  final def uniform2f(location: Token.UniformLocation, x: Float, y: Float): Unit = {
    ogl.uniform2f(location, x, y)
    this.checkError()
  }

  final def uniform2fv(location: Token.UniformLocation, values: FloatBuffer): Unit = {
    ogl.uniform2fv(location, values)
    this.checkError()
  }

  final def uniform2i(location: Token.UniformLocation, x: Int, y: Int): Unit = {
    ogl.uniform2i(location, x, y)
    this.checkError()
  }

  final def uniform2iv(location: Token.UniformLocation, values: IntBuffer): Unit = {
    ogl.uniform2iv(location, values)
    this.checkError()
  }

  final def uniform3f(location: Token.UniformLocation, x: Float, y: Float, z: Float): Unit = {
    ogl.uniform3f(location, x, y, z)
    this.checkError()
  }

  final def uniform3fv(location: Token.UniformLocation, values: FloatBuffer): Unit = {
    ogl.uniform3fv(location, values)
    this.checkError()
  }

  final def uniform3i(location: Token.UniformLocation, x: Int, y: Int, z: Int): Unit = {
    ogl.uniform3i(location, x, y, z)
    this.checkError()
  }

  final def uniform3iv(location: Token.UniformLocation, values: IntBuffer): Unit = {
    ogl.uniform3iv(location, values)
    this.checkError()
  }

  final def uniform4f(location: Token.UniformLocation, x: Float, y: Float, z: Float, w: Float): Unit = {
    ogl.uniform4f(location, x, y, z, w)
    this.checkError()
  }

  final def uniform4fv(location: Token.UniformLocation, values: FloatBuffer): Unit = {
    ogl.uniform4fv(location, values)
    this.checkError()
  }

  final def uniform4i(location: Token.UniformLocation, x: Int, y: Int, z: Int, w: Int): Unit = {
    ogl.uniform4i(location, x, y, z, w)
    this.checkError()
  }

  final def uniform4iv(location: Token.UniformLocation, values: IntBuffer): Unit = {
    ogl.uniform4iv(location, values)
    this.checkError()
  }

  final def uniformMatrix2fv(location: Token.UniformLocation, transpose: Boolean, matrices: FloatBuffer): Unit = {
    ogl.uniformMatrix2fv(location, transpose, matrices)
    this.checkError()
  }

  final def uniformMatrix3fv(location: Token.UniformLocation, transpose: Boolean, matrices: FloatBuffer): Unit = {
    ogl.uniformMatrix3fv(location, transpose, matrices)
    this.checkError()
  }

  final def uniformMatrix4fv(location: Token.UniformLocation, transpose: Boolean, matrices: FloatBuffer): Unit = {
    ogl.uniformMatrix4fv(location, transpose, matrices)
    this.checkError()
  }

  final def useProgram(program: Token.Program): Unit = {
    ogl.useProgram(program)
    this.checkError()
  }

  final def validateProgram(program: Token.Program): Unit = {
    ogl.validateProgram(program)
    this.checkError()

    // check for errors in the validation
    if (ogl.getProgramParameterb(program, GLES2.VALIDATE_STATUS) == false) {
      val msg = ogl.getProgramInfoLog(program)
      throw new GLException("Error in the validation of the program : " + msg)
    }
  }

  final def vertexAttrib1f(index: Int, x: Float): Unit = {
    ogl.vertexAttrib1f(index, x)
    this.checkError()
  }

  final def vertexAttrib1fv(index: Int, values: FloatBuffer): Unit = {
    ogl.vertexAttrib1fv(index, values)
    this.checkError()
  }

  final def vertexAttrib2f(index: Int, x: Float, y: Float): Unit = {
    ogl.vertexAttrib2f(index, x, y)
    this.checkError()
  }

  final def vertexAttrib2fv(index: Int, values: FloatBuffer): Unit = {
    ogl.vertexAttrib2fv(index, values)
    this.checkError()
  }

  final def vertexAttrib3f(index: Int, x: Float, y: Float, z: Float): Unit = {
    ogl.vertexAttrib3f(index, x, y, z)
    this.checkError()
  }

  final def vertexAttrib3fv(index: Int, values: FloatBuffer): Unit = {
    ogl.vertexAttrib3fv(index, values)
    this.checkError()
  }

  final def vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float): Unit = {
    ogl.vertexAttrib4f(index, x, y, z, w)
    this.checkError()
  }

  final def vertexAttrib4fv(index: Int, values: FloatBuffer): Unit = {
    ogl.vertexAttrib4fv(index, values)
    this.checkError()
  }

  final def vertexAttribPointer(index: Int, size: Int, `type`: Int, normalized: Boolean, stride: Int, offset: Long): Unit = {
    ogl.vertexAttribPointer(index, size, `type`, normalized, stride, offset)
    this.checkError()
  }
  final def vertexAttribPointer(index: Int, size: Int, `type`: Int, normalized: Boolean, stride: Int, offset: Int): Unit = {
    ogl.vertexAttribPointer(index, size, `type`, normalized, stride, offset)
    this.checkError()
  }

  final def viewport(x: Int, y: Int, width: Int, height: Int): Unit = {
    ogl.viewport(x, y, width, height)
    this.checkError()
  }

  // Helper methods

  final def validProgram(program: Token.Program): Boolean = {
    val ret = ogl.validProgram(program)
    this.checkError()
    ret
  }

  final def validShader(shader: Token.Shader): Boolean = {
    val ret = ogl.validShader(shader)
    this.checkError()
    ret
  }

  final def validBuffer(buffer: Token.Buffer): Boolean = {
    val ret = ogl.validBuffer(buffer)
    this.checkError()
    ret
  }

  final def validUniformLocation(uloc: Token.UniformLocation): Boolean = {
    val ret = ogl.validUniformLocation(uloc)
    this.checkError()
    ret
  }

  final def validFramebuffer(fb: Token.FrameBuffer): Boolean = {
    val ret = ogl.validFramebuffer(fb)
    this.checkError()
    ret
  }

  final def validRenderbuffer(rb: Token.RenderBuffer): Boolean = {
    val ret = ogl.validRenderbuffer(rb)
    this.checkError()
    ret
  }

}
