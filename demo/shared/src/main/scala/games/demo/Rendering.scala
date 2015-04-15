package games.demo

import scala.concurrent.{ Future, ExecutionContext }
import games.{ Utils, Resource }
import games.opengl.{ Token, GLES2 }
import games.math.{ Vector3f, Matrix4f }
import games.utils.SimpleOBJParser

import scala.collection.mutable
import scala.collection.immutable

case class OpenGLSubMesh(indicesBuffer: Token.Buffer, verticesCount: Int, ambientColor: Vector3f, diffuseColor: Vector3f, name: String)
case class OpenGLMesh(verticesBuffer: Token.Buffer, normalsBuffer: Token.Buffer, verticesCount: Int, subMeshes: Array[OpenGLSubMesh])

object Rendering {
  def loadAllShaders(resourceFolder: String, gl: GLES2, openglContext: ExecutionContext)(implicit ec: ExecutionContext): Future[Map[String, Token.Program]] = {
    loadAllFromList(resourceFolder, gl, openglContext, path => { loadShadersFromResourceFolder(path, gl, openglContext) })
  }

  def loadAllModels(resourceFolder: String, gl: GLES2, openglContext: ExecutionContext)(implicit ec: ExecutionContext): Future[Map[String, OpenGLMesh]] = {
    loadAllFromList(resourceFolder, gl, openglContext, path => { loadModelFromResourceFolder(path, gl, openglContext) })
  }

  def loadAllFromList[T](resourceFolder: String, gl: GLES2, openglContext: ExecutionContext, asyncGet: (String) => Future[T])(implicit ec: ExecutionContext): Future[Map[String, T]] = {
    val listResource = Resource(resourceFolder + "/list")
    val listFileFuture = Utils.getTextDataFromResource(listResource)
    listFileFuture.flatMap { listFile =>
      val lines = Utils.lines(listFile)
      val dataFutures = lines.map { line =>
        val dataResourcePath = resourceFolder + "/" + line
        asyncGet(dataResourcePath)
      }
      val datasFuture = Future.sequence(dataFutures.toSeq)
      datasFuture.map { seqShaders =>
        lines.zip(seqShaders).toMap
      }
    }
  }

  def loadShadersFromResourceFolder(resourceFolder: String, gl: GLES2, openglContext: ExecutionContext)(implicit ec: ExecutionContext): Future[Token.Program] = {
    val vertexResource = Resource(resourceFolder + "/vertex.c")
    val fragmentResource = Resource(resourceFolder + "/fragment.c")

    val vertexFileFuture = Utils.getTextDataFromResource(vertexResource)
    val fragmentFileFuture = Utils.getTextDataFromResource(fragmentResource)

    val filesFuture = Future.sequence(Seq(vertexFileFuture, fragmentFileFuture))

    filesFuture.map {
      case Seq(vertexFile, fragmentFile) =>
        val program = gl.createProgram()

        def compileShader(shaderType: Int, source: String): Token.Shader = {
          val shader = gl.createShader(shaderType)
          gl.shaderSource(shader, source)
          gl.compileShader(shader)

          // Check for compilation error
          if (gl.getShaderParameterb(shader, GLES2.COMPILE_STATUS) == false) {
            val msg = gl.getShaderInfoLog(shader)
            throw new RuntimeException("Error in the compilation of the shader: " + msg)
          }

          gl.attachShader(program, shader)
          shader
        }

        val vertexShader = compileShader(GLES2.VERTEX_SHADER, vertexFile)
        val fragmentShader = compileShader(GLES2.FRAGMENT_SHADER, fragmentFile)
        gl.linkProgram(program)

        // Check for linking error
        if (gl.getProgramParameterb(program, GLES2.LINK_STATUS) == false) {
          val msg = gl.getProgramInfoLog(program)
          throw new RuntimeException("Error in the linking of the program: " + msg)
        }

        gl.checkError()

        program
    }(openglContext)
  }

  def loadModelFromResourceFolder(resourceFolder: String, gl: GLES2, openglContext: ExecutionContext)(implicit ec: ExecutionContext): Future[OpenGLMesh] = {
    val mainResource = Resource(resourceFolder + "/main")
    val mainFileFuture = Utils.getTextDataFromResource(mainResource)
    mainFileFuture.flatMap { mainFile =>
      val mainLines = Utils.lines(mainFile)

      var nameOpt: Option[String] = None
      var objPathOpt: Option[String] = None
      val mtlPaths: mutable.Queue[String] = mutable.Queue()

      mainLines.foreach { line =>
        val tokens = line.split("=", 2)
        if (tokens.size != 2) throw new RuntimeException("Main model file malformed: \"" + line + "\"")
        val key = tokens(0)
        val value = tokens(1)

        key match {
          case "name" => nameOpt = Some(value)
          case "obj"  => objPathOpt = Some(value)
          case "mtl"  => mtlPaths += value
          case _      => Console.err.println("Warning: unknown model key in line: \"" + line + "\"")
        }
      }

      def missing(missingKey: String) = throw new RuntimeException("Missing key for " + missingKey + " in model")

      val name = nameOpt.getOrElse(missing("name"))
      val objPath = objPathOpt.getOrElse(missing("obj path"))

      val objResource = Resource(resourceFolder + "/" + objPath)
      val objFileFuture = Utils.getTextDataFromResource(objResource)

      val mtlFileFutures = for (mtlPath <- mtlPaths) yield {
        val mtlResource = Resource(resourceFolder + "/" + mtlPath)
        val mtlFileFuture = Utils.getTextDataFromResource(mtlResource)
        mtlFileFuture
      }

      val mtlFilesFuture = Future.sequence(mtlFileFutures)

      val meshFuture = for (
        objFile <- objFileFuture;
        mtlFiles <- mtlFilesFuture
      ) yield {
        val objLines = Utils.lines(objFile)
        val mtlLines = mtlPaths.zip(mtlFiles.map(Utils.lines(_))).toMap

        val objs = SimpleOBJParser.parseOBJ(objLines, mtlLines)
        val meshes = SimpleOBJParser.convOBJObjectToTriMesh(objs)

        val mesh = meshes(name)

        mesh
      }

      // Execute the loading part separately, in the OpenGL context
      meshFuture.map { mesh =>
        val meshVerticesCount = mesh.vertices.length
        val verticesData = GLES2.createFloatBuffer(meshVerticesCount * 3)
        mesh.vertices.foreach { v => v.store(verticesData) }
        assert(verticesData.remaining() == 0) // Sanity check
        verticesData.flip()
        val verticesBuffer = gl.createBuffer()
        gl.bindBuffer(GLES2.ARRAY_BUFFER, verticesBuffer)
        gl.bufferData(GLES2.ARRAY_BUFFER, verticesData, GLES2.STATIC_DRAW)
        val normals = mesh.normals.getOrElse(throw new RuntimeException("Missing normals"))
        assert(meshVerticesCount == normals.length) // Sanity check
        val normalsData = GLES2.createFloatBuffer(meshVerticesCount * 3)
        normals.foreach { v => v.store(normalsData) }
        assert(normalsData.remaining() == 0) // Sanity check
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
          assert(indicesData.remaining() == 0) // Sanity check
          indicesData.flip()
          val indicesBuffer = gl.createBuffer()
          gl.bindBuffer(GLES2.ELEMENT_ARRAY_BUFFER, indicesBuffer)
          gl.bufferData(GLES2.ELEMENT_ARRAY_BUFFER, indicesData, GLES2.STATIC_DRAW)
          val material = submesh.material.getOrElse(throw new RuntimeException("Missing material"))
          OpenGLSubMesh(indicesBuffer, submeshVerticesCount, material.ambientColor.getOrElse(throw new RuntimeException("Missing ambient color")), material.diffuseColor.getOrElse(throw new RuntimeException("Missing ambient color")), material.name)
        }

        gl.checkError()

        OpenGLMesh(verticesBuffer, normalsBuffer, meshVerticesCount, openGLSubMeshes)
      }(openglContext)
    }
  }

  def renderShip(playerId: Int, mesh: OpenGLMesh, transform: Matrix4f, cameraTransformInv: Matrix4f, gl: GLES2, positionAttrLoc: Int, normalAttrLoc: Int, modelViewUniLoc: Token.UniformLocation, modelViewInvTrUniLoc: Token.UniformLocation, diffuseColorUniLoc: Token.UniformLocation): Unit = {
    val modelView = cameraTransformInv * transform
    val modelViewInvTr = modelView.invertedCopy().transpose()

    gl.uniformMatrix4f(modelViewUniLoc, modelView)
    gl.uniformMatrix4f(modelViewInvTrUniLoc, modelViewInvTr)

    gl.bindBuffer(GLES2.ARRAY_BUFFER, mesh.verticesBuffer)
    gl.vertexAttribPointer(positionAttrLoc, 3, GLES2.FLOAT, false, 0, 0)
    gl.bindBuffer(GLES2.ARRAY_BUFFER, mesh.normalsBuffer)
    gl.vertexAttribPointer(normalAttrLoc, 3, GLES2.FLOAT, false, 0, 0)
    mesh.subMeshes.foreach { submesh =>
      val color = if (submesh.name == "[player]") Data.colors(playerId) else submesh.diffuseColor
      gl.uniform3f(diffuseColorUniLoc, color)
      gl.bindBuffer(GLES2.ELEMENT_ARRAY_BUFFER, submesh.indicesBuffer)
      gl.drawElements(GLES2.TRIANGLES, submesh.verticesCount, GLES2.UNSIGNED_SHORT, 0)
    }
  }
}
