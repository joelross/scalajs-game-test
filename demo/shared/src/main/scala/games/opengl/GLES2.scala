package games.opengl

import java.nio.{ ByteBuffer, ShortBuffer, IntBuffer, FloatBuffer, DoubleBuffer }

// Auxiliary components

case class ActiveInfo(size: Int, tpe: Int, name: String)
case class PrecisionFormat(rangeMin: Int, rangeMax: Int, precision: Int)

class GLException(msg: String) extends RuntimeException(msg)

// Main components

trait GLES2 {

  /* public API */

  def activeTexture(texture: Int): Unit

  def attachShader(program: Token.Program, shader: Token.Shader): Unit

  def bindAttribLocation(program: Token.Program, index: Int, name: String): Unit

  def bindBuffer(target: Int, buffer: Token.Buffer): Unit

  def bindFramebuffer(target: Int, framebuffer: Token.FrameBuffer): Unit

  def bindRenderbuffer(target: Int, renderbuffer: Token.RenderBuffer): Unit

  def bindTexture(target: Int, texture: Token.Texture): Unit

  def blendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit

  def blendEquation(mode: Int): Unit

  def blendEquationSeparate(modeRGB: Int, modeAlpha: Int): Unit

  def blendFunc(sfactor: Int, dfactor: Int): Unit

  def blendFuncSeparate(srcfactorRGB: Int, dstfactorRGB: Int, srcfactorAlpha: Int, dstfactorAlpha: Int): Unit

  def bufferData(target: Int, totalBytes: Long, usage: Int): Unit

  def bufferData(target: Int, data: ByteBuffer, usage: Int): Unit
  def bufferData(target: Int, data: ShortBuffer, usage: Int): Unit
  def bufferData(target: Int, data: IntBuffer, usage: Int): Unit
  def bufferData(target: Int, data: FloatBuffer, usage: Int): Unit
  def bufferData(target: Int, data: DoubleBuffer, usage: Int): Unit

  def bufferSubData(target: Int, offset: Long, data: ByteBuffer): Unit
  def bufferSubData(target: Int, offset: Long, data: ShortBuffer): Unit
  def bufferSubData(target: Int, offset: Long, data: IntBuffer): Unit
  def bufferSubData(target: Int, offset: Long, data: FloatBuffer): Unit
  def bufferSubData(target: Int, offset: Long, data: DoubleBuffer): Unit

  def checkFramebufferStatus(target: Int): Int

  def clear(mask: Int): Unit

  def clearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit

  def clearDepth(depth: Double): Unit

  def clearStencil(s: Int): Unit

  def colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit

  def compileShader(shader: Token.Shader): Unit

  /*
   * Method compressedTexImage2D with signature glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int data_imageSize, long data_buffer_offset) discarded
   * Reason: not available in the API WebGL and the API GLES20 of Android
   */

  def compressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                           data: ByteBuffer): Unit

  def compressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                              format: Int, data: ByteBuffer): Unit
  def copyTexImage2D(target: Int, level: Int, internalFormat: Int, x: Int, y: Int, width: Int, height: Int, border: Int): Unit

  def copyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit

  def createBuffer(): Token.Buffer

  def createFramebuffer(): Token.FrameBuffer

  def createProgram(): Token.Program

  def createRenderbuffer(): Token.RenderBuffer

  def createShader(`type`: Int): Token.Shader

  def createTexture(): Token.Texture

  def cullFace(mode: Int): Unit

  def deleteBuffer(buffer: Token.Buffer): Unit

  def deleteFramebuffer(framebuffer: Token.FrameBuffer): Unit

  def deleteProgram(program: Token.Program): Unit

  def deleteRenderbuffer(renderbuffer: Token.RenderBuffer): Unit

  def deleteShader(shader: Token.Shader): Unit

  def deleteTexture(texture: Token.Texture): Unit

  def depthFunc(func: Int): Unit

  def depthMask(flag: Boolean): Unit

  def depthRange(zNear: Double, zFar: Double): Unit

  def detachShader(program: Token.Program, shader: Token.Shader): Unit

  def disable(cap: Int): Unit

  def disableVertexAttribArray(index: Int): Unit

  def drawArrays(mode: Int, first: Int, count: Int): Unit

  /*
   * Method drawElements with signature glDrawElements(int mode, *Buffer indices) discarded
   * Reason: not available in the API WebGL
   * Note: available in the API GLES20 of Android with the signature glDrawElements(int mode, int count, int type, Buffer indices)
   * Note: the following available method requires the use of an element array buffer currently bound to ELEMENT_ARRAY_BUFFER
   */

  def drawElements(mode: Int, count: Int, `type`: Int, offset: Long): Unit

  def enable(cap: Int): Unit

  def enableVertexAttribArray(index: Int): Unit

  def finish(): Unit

  def flush(): Unit

  def framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Token.RenderBuffer): Unit

  def framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Token.Texture, level: Int): Unit

  def frontFace(mode: Int): Unit

  def generateMipmap(target: Int): Unit

  def getActiveAttrib(program: Token.Program, index: Int): ActiveInfo

  def getActiveUniform(program: Token.Program, index: Int): ActiveInfo

  def getAttachedShaders(program: Token.Program): Array[Token.Shader]

  def getAttribLocation(program: Token.Program, name: String): Int

  def getBufferParameteri(target: Int, pname: Int): Int

  /*
   * This set of function is a big mess around the different systems due to the fact that the returned value can be pretty
   * much anything.
   * 
   * On good old C, LWJGL and Android GLES20: glGet is divided between glGetBooleanv, glGetFloatv, glGetDoublev,
   * glGetIntegerv and glGetString.
   * Please note that it can actually store SEVERAL values in the provided pointer/buffer.
   * 
   * On WebGL, things get a bit tricky. There is a single function getParameter that just return something of type Any (yay...
   * the joy of dynamic typing).
   */

  def getParameterBuffer(pname: Int): Token.Buffer

  def getParameterTexture(pname: Int): Token.Texture

  def getParameterFramebuffer(pname: Int): Token.FrameBuffer

  def getParameterProgram(pname: Int): Token.Program

  def getParameterRenderbuffer(pname: Int): Token.RenderBuffer

  def getParameterShader(pname: Int): Token.Shader

  def getParameterString(pname: Int): String

  def getParameteri(pname: Int): Int

  def getParameteriv(pname: Int, outputs: IntBuffer): Unit

  def getParameterf(pname: Int): Float

  def getParameterfv(pname: Int, outputs: FloatBuffer): Unit

  def getParameterb(pname: Int): Boolean

  def getParameterbv(pname: Int, outputs: ByteBuffer): Unit

  def getError(): Int

  def getFramebufferAttachmentParameteri(target: Int, attachment: Int, pname: Int): Int

  def getFramebufferAttachmentParameterRenderbuffer(target: Int, attachment: Int, pname: Int): Token.RenderBuffer

  def getFramebufferAttachmentParameterTexture(target: Int, attachment: Int, pname: Int): Token.Texture

  def getProgramParameteri(program: Token.Program, pname: Int): Int

  def getProgramParameterb(program: Token.Program, pname: Int): Boolean

  def getProgramInfoLog(program: Token.Program): String

  def getRenderbufferParameteri(target: Int, pname: Int): Int

  def getShaderParameteri(shader: Token.Shader, pname: Int): Int

  def getShaderParameterb(shader: Token.Shader, pname: Int): Boolean

  def getShaderPrecisionFormat(shadertype: Int, precisiontype: Int): PrecisionFormat

  def getShaderInfoLog(shader: Token.Shader): String

  def getShaderSource(shader: Token.Shader): String

  def getTexParameteri(target: Int, pname: Int): Int

  def getUniformi(program: Token.Program, location: Token.UniformLocation): Int

  def getUniformiv(program: Token.Program, location: Token.UniformLocation, outputs: IntBuffer): Unit

  def getUniformf(program: Token.Program, location: Token.UniformLocation): Float

  def getUniformfv(program: Token.Program, location: Token.UniformLocation, outputs: FloatBuffer): Unit

  def getUniformLocation(program: Token.Program, name: String): Token.UniformLocation

  def getVertexAttribi(index: Int, pname: Int): Int

  def getVertexAttribf(index: Int, pname: Int): Float

  def getVertexAttribfv(index: Int, pname: Int, outputs: FloatBuffer): Unit

  def getVertexAttribb(index: Int, pname: Int): Boolean

  /*
   * Method glGetVertexAttribPointer discarded
   * Reason: not available in the API GLES20 of Android
   * Note: (partially) present with the name getVertexAttribOffset in the API WebGL (limited to retrieving only the
   * offset, not the pointer)
   */

  def hint(target: Int, mode: Int): Unit

  def isBuffer(buffer: Token.Buffer): Boolean

  def isEnabled(cap: Int): Boolean

  def isFramebuffer(framebuffer: Token.FrameBuffer): Boolean

  def isProgram(program: Token.Program): Boolean

  def isRenderbuffer(renderbuffer: Token.RenderBuffer): Boolean

  def isShader(shader: Token.Shader): Boolean

  def isTexture(texture: Token.Texture): Boolean

  def lineWidth(width: Float): Unit

  def linkProgram(program: Token.Program): Unit

  def pixelStorei(pname: Int, param: Int): Unit

  def polygonOffset(factor: Float, units: Float): Unit

  def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: ByteBuffer): Unit
  def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: ShortBuffer): Unit
  def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: IntBuffer): Unit
  def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: FloatBuffer): Unit
  def readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: DoubleBuffer): Unit

  def renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int): Unit

  def sampleCoverage(value: Float, invert: Boolean): Unit

  def scissor(x: Int, y: Int, width: Int, height: Int): Unit

  def shaderSource(shader: Token.Shader, source: String): Unit

  def stencilFunc(func: Int, ref: Int, mask: Int): Unit

  def stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int): Unit

  def stencilMask(mask: Int): Unit

  def stencilMaskSeparate(face: Int, mask: Int): Unit

  def stencilOp(fail: Int, zfail: Int, zpass: Int): Unit

  def stencilOpSeparate(face: Int, sfail: Int, dpfail: Int, dppass: Int): Unit

  def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                 format: Int, `type`: Int, pixels: ByteBuffer): Unit
  def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                 format: Int, `type`: Int, pixels: ShortBuffer): Unit
  def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                 format: Int, `type`: Int, pixels: IntBuffer): Unit
  def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                 format: Int, `type`: Int, pixels: FloatBuffer): Unit
  def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                 format: Int, `type`: Int, pixels: DoubleBuffer): Unit
  def texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int,
                 format: Int, `type`: Int): Unit

  def texParameterf(target: Int, pname: Int, param: Float): Unit

  def texParameteri(target: Int, pname: Int, param: Int): Unit

  def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                    format: Int, `type`: Int, pixels: ByteBuffer): Unit
  def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                    format: Int, `type`: Int, pixels: ShortBuffer): Unit
  def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                    format: Int, `type`: Int, pixels: IntBuffer): Unit
  def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                    format: Int, `type`: Int, pixels: FloatBuffer): Unit
  def texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int,
                    format: Int, `type`: Int, pixels: DoubleBuffer): Unit

  def uniform1f(location: Token.UniformLocation, x: Float): Unit

  def uniform1fv(location: Token.UniformLocation, values: FloatBuffer): Unit

  def uniform1i(location: Token.UniformLocation, x: Int): Unit

  def uniform1iv(location: Token.UniformLocation, values: IntBuffer): Unit

  def uniform2f(location: Token.UniformLocation, x: Float, y: Float): Unit

  def uniform2fv(location: Token.UniformLocation, values: FloatBuffer): Unit

  def uniform2i(location: Token.UniformLocation, x: Int, y: Int): Unit

  def uniform2iv(location: Token.UniformLocation, values: IntBuffer): Unit

  def uniform3f(location: Token.UniformLocation, x: Float, y: Float, z: Float): Unit

  def uniform3fv(location: Token.UniformLocation, values: FloatBuffer): Unit

  def uniform3i(location: Token.UniformLocation, x: Int, y: Int, z: Int): Unit

  def uniform3iv(location: Token.UniformLocation, values: IntBuffer): Unit

  def uniform4f(location: Token.UniformLocation, x: Float, y: Float, z: Float, w: Float): Unit

  def uniform4fv(location: Token.UniformLocation, values: FloatBuffer): Unit

  def uniform4i(location: Token.UniformLocation, x: Int, y: Int, z: Int, w: Int): Unit

  def uniform4iv(location: Token.UniformLocation, values: IntBuffer): Unit

  def uniformMatrix2fv(location: Token.UniformLocation, transpose: Boolean, matrices: FloatBuffer): Unit

  def uniformMatrix3fv(location: Token.UniformLocation, transpose: Boolean, matrices: FloatBuffer): Unit

  def uniformMatrix4fv(location: Token.UniformLocation, transpose: Boolean, matrices: FloatBuffer): Unit

  def useProgram(program: Token.Program): Unit

  def validateProgram(program: Token.Program): Unit

  def vertexAttrib1f(index: Int, x: Float): Unit

  def vertexAttrib1fv(index: Int, values: FloatBuffer): Unit

  def vertexAttrib2f(index: Int, x: Float, y: Float): Unit

  def vertexAttrib2fv(index: Int, values: FloatBuffer): Unit

  def vertexAttrib3f(index: Int, x: Float, y: Float, z: Float): Unit

  def vertexAttrib3fv(index: Int, values: FloatBuffer): Unit

  def vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float): Unit

  def vertexAttrib4fv(index: Int, values: FloatBuffer): Unit

  /*
   * Method vertexAttribPointer with signature glVertexAttribPointer(int index, int size, boolean normalized,
   * int stride, *Buffer buffer) discarded
   * Reason: not available in the API WebGL
   * Note: available in the API GLES20 of Android
   * Note: the following available method requires the use of an array buffer currently bound to ARRAY_BUFFER
   */

  def vertexAttribPointer(index: Int, size: Int, `type`: Int, normalized: Boolean, stride: Int, offset: Long): Unit

  def viewport(x: Int, y: Int, width: Int, height: Int): Unit

  protected val maxResultSize = 16
  protected val tmpByte = GLES2.createByteBuffer(maxResultSize)
  protected val tmpShort = GLES2.createShortBuffer(maxResultSize)
  protected val tmpInt = GLES2.createIntBuffer(maxResultSize)
  protected val tmpFloat = GLES2.createFloatBuffer(maxResultSize)
  protected val tmpDouble = GLES2.createDoubleBuffer(maxResultSize)

  // Helper methods

  final def checkError(): Unit = {
    val code = this.getError()
    if (code != GLES2.NO_ERROR) {
      val msg = this.errorMessage(code)
      throw new GLException("Error " + code + " : " + msg)
    }
  }

  def errorMessage(code: Int): String

  final def errorMessage(): String = {
    val code = this.getError()
    this.errorMessage(code)
  }

  final def getCurrentProgram(): Token.Program = {
    this.getParameterProgram(GLES2.CURRENT_PROGRAM)
  }

  final def getCurrentRenderbufferBinding(): Token.RenderBuffer = {
    this.getParameterRenderbuffer(GLES2.RENDERBUFFER_BINDING)
  }

  final def shaderSource(shader: Token.Shader, srcarray: Array[CharSequence]) {
    this.shaderSource(shader, srcarray.mkString("\n"))
  }

  def validProgram(program: Token.Program): Boolean

  def validShader(shader: Token.Shader): Boolean

  def validBuffer(buffer: Token.Buffer): Boolean

  def validUniformLocation(uloc: Token.UniformLocation): Boolean

  def validFramebuffer(fb: Token.FrameBuffer): Boolean

  def validRenderbuffer(rb: Token.RenderBuffer): Boolean

  def differentPrograms(p1: Token.Program, p2: Token.Program): Boolean

  final def uniform2f(location: Token.UniformLocation, vec: games.math.Vector2f): Unit = {
    this.uniform2f(location, vec.x, vec.y)
  }

  final def uniform3f(location: Token.UniformLocation, vec: games.math.Vector3f): Unit = {
    this.uniform3f(location, vec.x, vec.y, vec.z)
  }

  final def uniform4f(location: Token.UniformLocation, vec: games.math.Vector4f): Unit = {
    this.uniform4f(location, vec.x, vec.y, vec.z, vec.w)
  }

  def uniformMatrix2f(location: Token.UniformLocation, mat: games.math.Matrix2f): Unit = {
    this.tmpFloat.clear()
    mat.store(this.tmpFloat, games.math.ColumnMajor)
    this.tmpFloat.flip()
    this.uniformMatrix2fv(location, false, this.tmpFloat.slice)
  }

  def uniformMatrix3f(location: Token.UniformLocation, mat: games.math.Matrix3f): Unit = {
    this.tmpFloat.clear()
    mat.store(this.tmpFloat, games.math.ColumnMajor)
    this.tmpFloat.flip()
    this.uniformMatrix3fv(location, false, this.tmpFloat.slice)
  }

  def uniformMatrix4f(location: Token.UniformLocation, mat: games.math.Matrix4f): Unit = {
    this.tmpFloat.clear()
    mat.store(this.tmpFloat, games.math.ColumnMajor)
    this.tmpFloat.flip()
    this.uniformMatrix4fv(location, false, this.tmpFloat.slice)
  }
}

trait GLES2CompRequirements {

  /* public API - constants */

  final val bytesPerShort = 2
  final val bytesPerInt = 4
  final val bytesPerFloat = 4
  final val bytesPerDouble = 8

  val FALSE: Int
  val TRUE: Int

  val DEPTH_BUFFER_BIT: Int
  val STENCIL_BUFFER_BIT: Int
  val COLOR_BUFFER_BIT: Int
  val POINTS: Int
  val LINES: Int
  val LINE_LOOP: Int
  val LINE_STRIP: Int
  val TRIANGLES: Int
  val TRIANGLE_STRIP: Int
  val TRIANGLE_FAN: Int
  val ZERO: Int
  val ONE: Int
  val SRC_COLOR: Int
  val ONE_MINUS_SRC_COLOR: Int
  val SRC_ALPHA: Int
  val ONE_MINUS_SRC_ALPHA: Int
  val DST_ALPHA: Int
  val ONE_MINUS_DST_ALPHA: Int
  val DST_COLOR: Int
  val ONE_MINUS_DST_COLOR: Int
  val SRC_ALPHA_SATURATE: Int
  val FUNC_ADD: Int
  val BLEND_EQUATION: Int
  val BLEND_EQUATION_RGB: Int
  val BLEND_EQUATION_ALPHA: Int
  val FUNC_SUBTRACT: Int
  val FUNC_REVERSE_SUBTRACT: Int
  val BLEND_DST_RGB: Int
  val BLEND_SRC_RGB: Int
  val BLEND_DST_ALPHA: Int
  val BLEND_SRC_ALPHA: Int
  val CONSTANT_COLOR: Int
  val ONE_MINUS_CONSTANT_COLOR: Int
  val CONSTANT_ALPHA: Int
  val ONE_MINUS_CONSTANT_ALPHA: Int
  val BLEND_COLOR: Int
  val ARRAY_BUFFER: Int
  val ELEMENT_ARRAY_BUFFER: Int
  val ARRAY_BUFFER_BINDING: Int
  val ELEMENT_ARRAY_BUFFER_BINDING: Int
  val STREAM_DRAW: Int
  val STATIC_DRAW: Int
  val DYNAMIC_DRAW: Int
  val BUFFER_SIZE: Int
  val BUFFER_USAGE: Int
  val CURRENT_VERTEX_ATTRIB: Int
  val FRONT: Int
  val BACK: Int
  val FRONT_AND_BACK: Int
  val CULL_FACE: Int
  val BLEND: Int
  val DITHER: Int
  val STENCIL_TEST: Int
  val DEPTH_TEST: Int
  val SCISSOR_TEST: Int
  val POLYGON_OFFSET_FILL: Int
  val SAMPLE_ALPHA_TO_COVERAGE: Int
  val SAMPLE_COVERAGE: Int
  val NO_ERROR: Int
  val INVALID_ENUM: Int
  val INVALID_VALUE: Int
  val INVALID_OPERATION: Int
  val OUT_OF_MEMORY: Int
  val CW: Int
  val CCW: Int
  val LINE_WIDTH: Int
  val ALIASED_POINT_SIZE_RANGE: Int
  val ALIASED_LINE_WIDTH_RANGE: Int
  val CULL_FACE_MODE: Int
  val FRONT_FACE: Int
  val DEPTH_RANGE: Int
  val DEPTH_WRITEMASK: Int
  val DEPTH_CLEAR_VALUE: Int
  val DEPTH_FUNC: Int
  val STENCIL_CLEAR_VALUE: Int
  val STENCIL_FUNC: Int
  val STENCIL_FAIL: Int
  val STENCIL_PASS_DEPTH_FAIL: Int
  val STENCIL_PASS_DEPTH_PASS: Int
  val STENCIL_REF: Int
  val STENCIL_VALUE_MASK: Int
  val STENCIL_WRITEMASK: Int
  val STENCIL_BACK_FUNC: Int
  val STENCIL_BACK_FAIL: Int
  val STENCIL_BACK_PASS_DEPTH_FAIL: Int
  val STENCIL_BACK_PASS_DEPTH_PASS: Int
  val STENCIL_BACK_REF: Int
  val STENCIL_BACK_VALUE_MASK: Int
  val STENCIL_BACK_WRITEMASK: Int
  val VIEWPORT: Int
  val SCISSOR_BOX: Int
  val COLOR_CLEAR_VALUE: Int
  val COLOR_WRITEMASK: Int
  val UNPACK_ALIGNMENT: Int
  val PACK_ALIGNMENT: Int
  val MAX_TEXTURE_SIZE: Int
  val MAX_VIEWPORT_DIMS: Int
  val SUBPIXEL_BITS: Int
  val RED_BITS: Int
  val GREEN_BITS: Int
  val BLUE_BITS: Int
  val ALPHA_BITS: Int
  val DEPTH_BITS: Int
  val STENCIL_BITS: Int
  val POLYGON_OFFSET_UNITS: Int
  val POLYGON_OFFSET_FACTOR: Int
  val TEXTURE_BINDING_2D: Int
  val SAMPLE_BUFFERS: Int
  val SAMPLES: Int
  val SAMPLE_COVERAGE_VALUE: Int
  val SAMPLE_COVERAGE_INVERT: Int
  val COMPRESSED_TEXTURE_FORMATS: Int
  val DONT_CARE: Int
  val FASTEST: Int
  val NICEST: Int
  val GENERATE_MIPMAP_HINT: Int
  val BYTE: Int
  val UNSIGNED_BYTE: Int
  val SHORT: Int
  val UNSIGNED_SHORT: Int
  val INT: Int
  val UNSIGNED_INT: Int
  val FLOAT: Int
  val DEPTH_COMPONENT: Int
  val ALPHA: Int
  val RGB: Int
  val RGBA: Int
  val LUMINANCE: Int
  val LUMINANCE_ALPHA: Int
  val UNSIGNED_SHORT_4_4_4_4: Int
  val UNSIGNED_SHORT_5_5_5_1: Int
  val UNSIGNED_SHORT_5_6_5: Int
  val FRAGMENT_SHADER: Int
  val VERTEX_SHADER: Int
  val MAX_VERTEX_ATTRIBS: Int
  val MAX_VERTEX_UNIFORM_VECTORS: Int
  val MAX_VARYING_VECTORS: Int
  val MAX_COMBINED_TEXTURE_IMAGE_UNITS: Int
  val MAX_VERTEX_TEXTURE_IMAGE_UNITS: Int
  val MAX_TEXTURE_IMAGE_UNITS: Int
  val MAX_FRAGMENT_UNIFORM_VECTORS: Int
  val SHADER_TYPE: Int
  val DELETE_STATUS: Int
  val LINK_STATUS: Int
  val VALIDATE_STATUS: Int
  val ATTACHED_SHADERS: Int
  val ACTIVE_UNIFORMS: Int
  val ACTIVE_ATTRIBUTES: Int
  val SHADING_LANGUAGE_VERSION: Int
  val CURRENT_PROGRAM: Int
  val NEVER: Int
  val LESS: Int
  val EQUAL: Int
  val LEQUAL: Int
  val GREATER: Int
  val NOTEQUAL: Int
  val GEQUAL: Int
  val ALWAYS: Int
  val KEEP: Int
  val REPLACE: Int
  val INCR: Int
  val DECR: Int
  val INVERT: Int
  val INCR_WRAP: Int
  val DECR_WRAP: Int
  val VENDOR: Int
  val RENDERER: Int
  val VERSION: Int
  val NEAREST: Int
  val LINEAR: Int
  val NEAREST_MIPMAP_NEAREST: Int
  val LINEAR_MIPMAP_NEAREST: Int
  val NEAREST_MIPMAP_LINEAR: Int
  val LINEAR_MIPMAP_LINEAR: Int
  val TEXTURE_MAG_FILTER: Int
  val TEXTURE_MIN_FILTER: Int
  val TEXTURE_WRAP_S: Int
  val TEXTURE_WRAP_T: Int
  val TEXTURE_2D: Int
  val TEXTURE: Int
  val TEXTURE_CUBE_MAP: Int
  val TEXTURE_BINDING_CUBE_MAP: Int
  val TEXTURE_CUBE_MAP_POSITIVE_X: Int
  val TEXTURE_CUBE_MAP_NEGATIVE_X: Int
  val TEXTURE_CUBE_MAP_POSITIVE_Y: Int
  val TEXTURE_CUBE_MAP_NEGATIVE_Y: Int
  val TEXTURE_CUBE_MAP_POSITIVE_Z: Int
  val TEXTURE_CUBE_MAP_NEGATIVE_Z: Int
  val MAX_CUBE_MAP_TEXTURE_SIZE: Int
  val TEXTURE0: Int
  val TEXTURE1: Int
  val TEXTURE2: Int
  val TEXTURE3: Int
  val TEXTURE4: Int
  val TEXTURE5: Int
  val TEXTURE6: Int
  val TEXTURE7: Int
  val TEXTURE8: Int
  val TEXTURE9: Int
  val TEXTURE10: Int
  val TEXTURE11: Int
  val TEXTURE12: Int
  val TEXTURE13: Int
  val TEXTURE14: Int
  val TEXTURE15: Int
  val TEXTURE16: Int
  val TEXTURE17: Int
  val TEXTURE18: Int
  val TEXTURE19: Int
  val TEXTURE20: Int
  val TEXTURE21: Int
  val TEXTURE22: Int
  val TEXTURE23: Int
  val TEXTURE24: Int
  val TEXTURE25: Int
  val TEXTURE26: Int
  val TEXTURE27: Int
  val TEXTURE28: Int
  val TEXTURE29: Int
  val TEXTURE30: Int
  val TEXTURE31: Int
  val ACTIVE_TEXTURE: Int
  val REPEAT: Int
  val CLAMP_TO_EDGE: Int
  val MIRRORED_REPEAT: Int
  val FLOAT_VEC2: Int
  val FLOAT_VEC3: Int
  val FLOAT_VEC4: Int
  val INT_VEC2: Int
  val INT_VEC3: Int
  val INT_VEC4: Int
  val BOOL: Int
  val BOOL_VEC2: Int
  val BOOL_VEC3: Int
  val BOOL_VEC4: Int
  val FLOAT_MAT2: Int
  val FLOAT_MAT3: Int
  val FLOAT_MAT4: Int
  val SAMPLER_2D: Int
  val SAMPLER_CUBE: Int
  val VERTEX_ATTRIB_ARRAY_ENABLED: Int
  val VERTEX_ATTRIB_ARRAY_SIZE: Int
  val VERTEX_ATTRIB_ARRAY_STRIDE: Int
  val VERTEX_ATTRIB_ARRAY_TYPE: Int
  val VERTEX_ATTRIB_ARRAY_NORMALIZED: Int
  val VERTEX_ATTRIB_ARRAY_POINTER: Int
  val VERTEX_ATTRIB_ARRAY_BUFFER_BINDING: Int
  val COMPILE_STATUS: Int
  val LOW_FLOAT: Int
  val MEDIUM_FLOAT: Int
  val HIGH_FLOAT: Int
  val LOW_INT: Int
  val MEDIUM_INT: Int
  val HIGH_INT: Int
  val FRAMEBUFFER: Int
  val RENDERBUFFER: Int
  val RGBA4: Int
  val RGB5_A1: Int
  val RGB565: Int
  val DEPTH_COMPONENT16: Int
  val STENCIL_INDEX: Int
  val STENCIL_INDEX8: Int
  val DEPTH_STENCIL: Int
  val RENDERBUFFER_WIDTH: Int
  val RENDERBUFFER_HEIGHT: Int
  val RENDERBUFFER_INTERNAL_FORMAT: Int
  val RENDERBUFFER_RED_SIZE: Int
  val RENDERBUFFER_GREEN_SIZE: Int
  val RENDERBUFFER_BLUE_SIZE: Int
  val RENDERBUFFER_ALPHA_SIZE: Int
  val RENDERBUFFER_DEPTH_SIZE: Int
  val RENDERBUFFER_STENCIL_SIZE: Int
  val FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE: Int
  val FRAMEBUFFER_ATTACHMENT_OBJECT_NAME: Int
  val FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL: Int
  val FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE: Int
  val COLOR_ATTACHMENT0: Int
  val DEPTH_ATTACHMENT: Int
  val STENCIL_ATTACHMENT: Int
  val DEPTH_STENCIL_ATTACHMENT: Int
  val NONE: Int
  val FRAMEBUFFER_COMPLETE: Int
  val FRAMEBUFFER_INCOMPLETE_ATTACHMENT: Int
  val FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT: Int
  val FRAMEBUFFER_INCOMPLETE_DIMENSIONS: Int
  // GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER not present in WebGL & Android, moved to Macroglex
  // GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER not present in WebGL & Android, moved to Macroglex
  val FRAMEBUFFER_UNSUPPORTED: Int
  val FRAMEBUFFER_BINDING: Int
  val RENDERBUFFER_BINDING: Int
  val MAX_RENDERBUFFER_SIZE: Int
  val INVALID_FRAMEBUFFER_OPERATION: Int

  /* public API - methods */

  def createByteBuffer(sz: Int): ByteBuffer
  def createShortBuffer(sz: Int): ShortBuffer
  def createIntBuffer(sz: Int): IntBuffer
  def createFloatBuffer(sz: Int): FloatBuffer
  def createDoubleBuffer(sz: Int): DoubleBuffer
}

object GLES2 extends GLES2CompImpl {}

