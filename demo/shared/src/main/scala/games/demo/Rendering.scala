package games.demo

import scala.concurrent.{ Future, ExecutionContext }
import games.{ Utils, Resource }
import games.opengl.{ Token, GLES2 }
import games.math._
import games.utils.SimpleOBJParser

import scala.collection.mutable
import scala.collection.immutable

case class OpenGLSubMesh(indicesBuffer: Token.Buffer, verticesCount: Int, ambientColor: Vector3f, diffuseColor: Vector3f, name: String)
case class OpenGLMesh(verticesBuffer: Token.Buffer, normalsBuffer: Token.Buffer, verticesCount: Int, subMeshes: Array[OpenGLSubMesh])

object Rendering {
  def validAttribLocation(aloc: Int): Boolean = aloc >= 0

  def loadAllShaders(resourceFolder: String, gl: GLES2, openglContext: ExecutionContext)(implicit ec: ExecutionContext): Future[immutable.Map[String, Token.Program]] = {
    loadAllFromList(resourceFolder, gl, openglContext, path => { loadShadersFromResourceFolder(path, gl, openglContext) })
  }

  def loadAllModels(resourceFolder: String, gl: GLES2, openglContext: ExecutionContext)(implicit ec: ExecutionContext): Future[immutable.Map[String, OpenGLMesh]] = {
    loadAllFromList(resourceFolder, gl, openglContext, path => { loadModelFromResourceFolder(path, gl, openglContext) })
  }

  def loadAllFromList[T](resourceFolder: String, gl: GLES2, openglContext: ExecutionContext, asyncGet: (String) => Future[T])(implicit ec: ExecutionContext): Future[immutable.Map[String, T]] = {
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

  def loadTriMeshFromResourceFolder(resourceFolder: String, gl: GLES2, openglContext: ExecutionContext)(implicit ec: ExecutionContext): Future[games.utils.SimpleOBJParser.TriMesh] = {
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

      for (
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
    }
  }

  var projection: Matrix4f = new Matrix4f

  private val fovy: Float = 60f // vertical field of view: 60°
  // render between 1cm and 1km
  private val near: Float = 0.01f
  private val far: Float = 1000f

  def setProjection(width: Int, height: Int)(implicit gl: GLES2): Unit = {
    gl.viewport(0, 0, width, height)
    Matrix4f.setPerspective3D(fovy, width.toFloat / height.toFloat, near, far, projection)
  }

  object Player {
    var mesh: OpenGLMesh = _

    def setup(mesh: OpenGLMesh)(implicit gl: GLES2): Unit = {
      this.mesh = mesh
    }
  }

  object Bullet {
    var mesh: OpenGLMesh = _

    def setup(mesh: OpenGLMesh)(implicit gl: GLES2): Unit = {
      this.mesh = mesh
    }
  }

  object Map {
    var program: Token.Program = _

    var wallVerticesBuffer: Token.Buffer = _
    var wallNormalsBuffer: Token.Buffer = _
    var wallIndicesBufferBySubmesh: Array[Token.Buffer] = _
    var wallRenderCountBySubmesh: Array[Int] = _
    var wallMaterialBySubmesh: Array[games.utils.SimpleOBJParser.Material] = _
    var wallSubmeshCount: Int = _

    var floorVerticesBuffer: Token.Buffer = _
    var floorNormalsBuffer: Token.Buffer = _
    var floorIndicesBufferBySubmesh: Array[Token.Buffer] = _
    var floorRenderCountBySubmesh: Array[Int] = _
    var floorMaterialBySubmesh: Array[games.utils.SimpleOBJParser.Material] = _
    var floorSubmeshCount: Int = _

    var positionAttrLoc: Int = _
    var normalAttrLoc: Int = _
    var ambientColorUniLoc: Token.UniformLocation = _
    var diffuseColorUniLoc: Token.UniformLocation = _
    var projectionUniLoc: Token.UniformLocation = _
    var modelViewUniLoc: Token.UniformLocation = _
    var normalModelViewUniLoc: Token.UniformLocation = _

    private def setupFloor(mesh: games.utils.SimpleOBJParser.TriMesh, map: Map)(implicit gl: GLES2): Unit = {
      val vertices = mesh.vertices
      val normals = mesh.normals.get

      assert(vertices.length == normals.length) // sanity check

      this.floorSubmeshCount = mesh.submeshes.length

      val entityCount = map.floors.length
      val entityTrisCountBySubmesh = mesh.submeshes.map(_.tris.length)

      val globalVerticesCount = entityCount * vertices.length
      val globalNormalsCount = entityCount * normals.length
      val globalTrisCountBySubmesh = entityTrisCountBySubmesh.map { trisCount => entityCount * trisCount }

      this.floorRenderCountBySubmesh = globalTrisCountBySubmesh.map { trisCount => trisCount * 3 }

      this.floorMaterialBySubmesh = mesh.submeshes.map { submesh => submesh.material.get }

      this.floorRenderCountBySubmesh.foreach { renderCount =>
        assert(renderCount <= Short.MaxValue) // Sanity check, make sure the indexing will be within short limits (could use "<= 0xFFFF" as the short are unsigned in latter opengl)
      }

      val globalVerticesData = GLES2.createFloatBuffer(globalVerticesCount * 3) // 3 floats (x, y, z) per vertex
      val globalNormalsData = GLES2.createFloatBuffer(globalNormalsCount * 3) // 3 floats (x, y, z) per normal
      val globalIndicesDataBySubmesh = globalTrisCountBySubmesh.map { trisCount => GLES2.createShortBuffer(trisCount * 3) } // 3 indices (vertices) per triangle

      var indicesOffset = 0

      for (floor <- map.floors) {
        val pos2d = floor
        val pos3d = new Vector3f(pos2d.x, 0f, pos2d.y)

        val transform = Matrix4f.translate3D(pos3d)
        val normalTransform = transform.toCartesian().invertedCopy().transposedCopy()

        for (vertex <- vertices) {
          val transformedVertex = (transform * vertex.toHomogeneous()).toCartesian()
          transformedVertex.store(globalVerticesData)
        }
        for (normal <- normals) {
          val transformedNormal = (normalTransform * normal).normalizedCopy()
          transformedNormal.store(globalNormalsData)
        }
        for (i <- 0 until this.floorSubmeshCount) {
          val submesh = mesh.submeshes(i)
          val globalIndicesData = globalIndicesDataBySubmesh(i)

          val tris = submesh.tris

          for ((i0, i1, i2) <- tris) {
            globalIndicesData.put((i0 + indicesOffset).toShort)
            globalIndicesData.put((i1 + indicesOffset).toShort)
            globalIndicesData.put((i2 + indicesOffset).toShort)
          }
        }
        indicesOffset += vertices.length
      }

      assert(globalVerticesData.remaining() == 0) // sanity check
      assert(globalNormalsData.remaining() == 0) // sanity check
      globalIndicesDataBySubmesh.foreach { globalIndicesData =>
        assert(globalIndicesData.remaining() == 0) // sanity check
      }

      globalVerticesData.flip()
      globalNormalsData.flip()
      globalIndicesDataBySubmesh.foreach { indicesData => indicesData.flip() }

      val globalVerticesBuffer = gl.createBuffer()
      val globalNormalsBuffer = gl.createBuffer()
      val globalIndicesBufferBySubmesh = mesh.submeshes.map { _ => gl.createBuffer() }

      gl.bindBuffer(GLES2.ARRAY_BUFFER, globalVerticesBuffer)
      gl.bufferData(GLES2.ARRAY_BUFFER, globalVerticesData, GLES2.STATIC_DRAW)

      gl.bindBuffer(GLES2.ARRAY_BUFFER, globalNormalsBuffer)
      gl.bufferData(GLES2.ARRAY_BUFFER, globalNormalsData, GLES2.STATIC_DRAW)

      for (i <- 0 until this.floorSubmeshCount) {
        val globalIndicesBuffer = globalIndicesBufferBySubmesh(i)
        val globalIndicesData = globalIndicesDataBySubmesh(i)

        gl.bindBuffer(GLES2.ELEMENT_ARRAY_BUFFER, globalIndicesBuffer)
        gl.bufferData(GLES2.ELEMENT_ARRAY_BUFFER, globalIndicesData, GLES2.STATIC_DRAW)
      }

      this.floorVerticesBuffer = globalVerticesBuffer
      this.floorNormalsBuffer = globalNormalsBuffer
      this.floorIndicesBufferBySubmesh = globalIndicesBufferBySubmesh
    }

    private def setupWall(mesh: games.utils.SimpleOBJParser.TriMesh, map: Map)(implicit gl: GLES2): Unit = {
      val vertices = mesh.vertices
      val normals = mesh.normals.get

      assert(vertices.length == normals.length) // sanity check

      this.wallSubmeshCount = mesh.submeshes.length

      val entityCount = (map.lWalls.length + map.rWalls.length + map.tWalls.length + map.bWalls.length)
      val entityTrisCountBySubmesh = mesh.submeshes.map(_.tris.length)

      val globalVerticesCount = entityCount * vertices.length
      val globalNormalsCount = entityCount * normals.length
      val globalTrisCountBySubmesh = entityTrisCountBySubmesh.map { trisCount => entityCount * trisCount }

      this.wallRenderCountBySubmesh = globalTrisCountBySubmesh.map { trisCount => trisCount * 3 }

      this.wallMaterialBySubmesh = mesh.submeshes.map { submesh => submesh.material.get }

      this.wallRenderCountBySubmesh.foreach { renderCount =>
        assert(renderCount <= Short.MaxValue) // Sanity check, make sure the indexing will be within short limits (could use "<= 0xFFFF" as the short are unsigned in latter opengl)
      }

      val globalVerticesData = GLES2.createFloatBuffer(globalVerticesCount * 3) // 3 floats (x, y, z) per vertex
      val globalNormalsData = GLES2.createFloatBuffer(globalNormalsCount * 3) // 3 floats (x, y, z) per normal
      val globalIndicesDataBySubmesh = globalTrisCountBySubmesh.map { trisCount => GLES2.createShortBuffer(trisCount * 3) } // 3 indices (vertices) per triangle

      var indicesOffset = 0

      def extractWalls(walls: Array[Vector2f], orientation: Float): Unit = {
        val wallTransform = Matrix4f.rotate3D(orientation, Vector3f.Up)
        for (wall <- walls) {
          val pos2d = wall
          val pos3d = new Vector3f(pos2d.x, 0f, pos2d.y)

          val transform = Matrix4f.translate3D(pos3d) * wallTransform
          val normalTransform = transform.toCartesian().invertedCopy().transposedCopy()

          for (vertex <- vertices) {
            val transformedVertex = (transform * vertex.toHomogeneous()).toCartesian()
            transformedVertex.store(globalVerticesData)
          }
          for (normal <- normals) {
            val transformedNormal = (normalTransform * normal).normalizedCopy()
            transformedNormal.store(globalNormalsData)
          }
          for (i <- 0 until this.wallSubmeshCount) {
            val submesh = mesh.submeshes(i)
            val globalIndicesData = globalIndicesDataBySubmesh(i)

            val tris = submesh.tris

            for ((i0, i1, i2) <- tris) {
              globalIndicesData.put((i0 + indicesOffset).toShort)
              globalIndicesData.put((i1 + indicesOffset).toShort)
              globalIndicesData.put((i2 + indicesOffset).toShort)
            }
          }
          indicesOffset += vertices.length
        }
      }

      extractWalls(map.lWalls, 270f)
      extractWalls(map.rWalls, 90f)
      extractWalls(map.tWalls, 180f)
      extractWalls(map.bWalls, 0f)

      assert(globalVerticesData.remaining() == 0) // sanity check
      assert(globalNormalsData.remaining() == 0) // sanity check
      globalIndicesDataBySubmesh.foreach { globalIndicesData =>
        assert(globalIndicesData.remaining() == 0) // sanity check
      }

      globalVerticesData.flip()
      globalNormalsData.flip()
      globalIndicesDataBySubmesh.foreach { indicesData => indicesData.flip() }

      val globalVerticesBuffer = gl.createBuffer()
      val globalNormalsBuffer = gl.createBuffer()
      val globalIndicesBufferBySubmesh = mesh.submeshes.map { _ => gl.createBuffer() }

      gl.bindBuffer(GLES2.ARRAY_BUFFER, globalVerticesBuffer)
      gl.bufferData(GLES2.ARRAY_BUFFER, globalVerticesData, GLES2.STATIC_DRAW)

      gl.bindBuffer(GLES2.ARRAY_BUFFER, globalNormalsBuffer)
      gl.bufferData(GLES2.ARRAY_BUFFER, globalNormalsData, GLES2.STATIC_DRAW)

      for (i <- 0 until this.wallSubmeshCount) {
        val globalIndicesBuffer = globalIndicesBufferBySubmesh(i)
        val globalIndicesData = globalIndicesDataBySubmesh(i)

        gl.bindBuffer(GLES2.ELEMENT_ARRAY_BUFFER, globalIndicesBuffer)
        gl.bufferData(GLES2.ELEMENT_ARRAY_BUFFER, globalIndicesData, GLES2.STATIC_DRAW)
      }

      this.wallVerticesBuffer = globalVerticesBuffer
      this.wallNormalsBuffer = globalNormalsBuffer
      this.wallIndicesBufferBySubmesh = globalIndicesBufferBySubmesh
    }

    def setup(program: Token.Program, wallMesh: games.utils.SimpleOBJParser.TriMesh, floorMesh: games.utils.SimpleOBJParser.TriMesh, map: Map)(implicit gl: GLES2): Unit = {
      this.program = program

      this.positionAttrLoc = gl.getAttribLocation(program, "position")
      this.normalAttrLoc = gl.getAttribLocation(program, "normal")

      this.ambientColorUniLoc = gl.getUniformLocation(program, "ambientColor")
      this.diffuseColorUniLoc = gl.getUniformLocation(program, "diffuseColor")
      this.projectionUniLoc = gl.getUniformLocation(program, "projection")
      this.modelViewUniLoc = gl.getUniformLocation(program, "modelView")
      this.normalModelViewUniLoc = gl.getUniformLocation(program, "normalModelView")

      setupWall(wallMesh, map)
      setupFloor(floorMesh, map)

      gl.checkError()
    }

    def init()(implicit gl: GLES2): Unit = {
      gl.useProgram(program)
      gl.uniformMatrix4f(projectionUniLoc, projection)

      gl.enableVertexAttribArray(positionAttrLoc)
      gl.enableVertexAttribArray(normalAttrLoc)
    }

    def close()(implicit gl: GLES2): Unit = {
      gl.disableVertexAttribArray(normalAttrLoc)
      gl.disableVertexAttribArray(positionAttrLoc)
    }

    def render(cameraTransformInv: Matrix4f)(implicit gl: GLES2): Unit = {
      val modelView = cameraTransformInv
      val normalModelView = modelView.toCartesian().invertedCopy().transpose()

      gl.uniformMatrix4f(modelViewUniLoc, modelView)
      gl.uniformMatrix3f(normalModelViewUniLoc, normalModelView)

      // Walls
      gl.bindBuffer(GLES2.ARRAY_BUFFER, this.wallVerticesBuffer)
      gl.vertexAttribPointer(positionAttrLoc, 3, GLES2.FLOAT, false, 0, 0)

      gl.bindBuffer(GLES2.ARRAY_BUFFER, this.wallNormalsBuffer)
      gl.vertexAttribPointer(normalAttrLoc, 3, GLES2.FLOAT, false, 0, 0)

      for (i <- 0 until this.wallSubmeshCount) {
        val indicesBuffer = this.wallIndicesBufferBySubmesh(i)
        val renderCount = this.wallRenderCountBySubmesh(i)
        val material = this.wallMaterialBySubmesh(i)

        val ambientColor = material.ambientColor.get
        gl.uniform3f(ambientColorUniLoc, ambientColor)

        val diffuseColor = material.diffuseColor.get
        gl.uniform3f(diffuseColorUniLoc, diffuseColor)

        gl.bindBuffer(GLES2.ELEMENT_ARRAY_BUFFER, indicesBuffer)
        gl.drawElements(GLES2.TRIANGLES, renderCount, GLES2.UNSIGNED_SHORT, 0)
      }

      // Floors
      gl.bindBuffer(GLES2.ARRAY_BUFFER, this.floorVerticesBuffer)
      gl.vertexAttribPointer(positionAttrLoc, 3, GLES2.FLOAT, false, 0, 0)

      gl.bindBuffer(GLES2.ARRAY_BUFFER, this.floorNormalsBuffer)
      gl.vertexAttribPointer(normalAttrLoc, 3, GLES2.FLOAT, false, 0, 0)

      for (i <- 0 until this.floorSubmeshCount) {
        val indicesBuffer = this.floorIndicesBufferBySubmesh(i)
        val renderCount = this.floorRenderCountBySubmesh(i)
        val material = this.floorMaterialBySubmesh(i)

        val ambientColor = material.ambientColor.get
        gl.uniform3f(ambientColorUniLoc, ambientColor)

        val diffuseColor = material.diffuseColor.get
        gl.uniform3f(diffuseColorUniLoc, diffuseColor)

        gl.bindBuffer(GLES2.ELEMENT_ARRAY_BUFFER, indicesBuffer)
        gl.drawElements(GLES2.TRIANGLES, renderCount, GLES2.UNSIGNED_SHORT, 0)
      }
    }
  }

  object Standard {
    var program: Token.Program = _

    var positionAttrLoc: Int = _
    var normalAttrLoc: Int = _
    var ambientColorUniLoc: Token.UniformLocation = _
    var diffuseColorUniLoc: Token.UniformLocation = _
    var projectionUniLoc: Token.UniformLocation = _
    var modelViewUniLoc: Token.UniformLocation = _
    var normalModelViewUniLoc: Token.UniformLocation = _

    def setup(program: Token.Program)(implicit gl: GLES2): Unit = {
      this.program = program

      positionAttrLoc = gl.getAttribLocation(program, "position")
      normalAttrLoc = gl.getAttribLocation(program, "normal")

      ambientColorUniLoc = gl.getUniformLocation(program, "ambientColor")
      diffuseColorUniLoc = gl.getUniformLocation(program, "diffuseColor")
      projectionUniLoc = gl.getUniformLocation(program, "projection")
      modelViewUniLoc = gl.getUniformLocation(program, "modelView")
      normalModelViewUniLoc = gl.getUniformLocation(program, "normalModelView")
    }

    def init()(implicit gl: GLES2): Unit = {
      gl.useProgram(program)
      gl.uniformMatrix4f(projectionUniLoc, projection)

      gl.enableVertexAttribArray(positionAttrLoc)
      gl.enableVertexAttribArray(normalAttrLoc)
    }

    def close()(implicit gl: GLES2): Unit = {
      gl.disableVertexAttribArray(normalAttrLoc)
      gl.disableVertexAttribArray(positionAttrLoc)
    }

    def render(playerId: Int, mesh: OpenGLMesh, transform: Matrix4f, cameraTransformInv: Matrix4f)(implicit gl: GLES2): Unit = {
      val modelView = cameraTransformInv * transform
      val normalModelView = modelView.toCartesian().invertedCopy().transpose()

      gl.uniformMatrix4f(modelViewUniLoc, modelView)
      gl.uniformMatrix3f(normalModelViewUniLoc, normalModelView)

      gl.bindBuffer(GLES2.ARRAY_BUFFER, mesh.verticesBuffer)
      gl.vertexAttribPointer(positionAttrLoc, 3, GLES2.FLOAT, false, 0, 0)
      gl.bindBuffer(GLES2.ARRAY_BUFFER, mesh.normalsBuffer)
      gl.vertexAttribPointer(normalAttrLoc, 3, GLES2.FLOAT, false, 0, 0)
      mesh.subMeshes.foreach { submesh =>
        val (ambientColor, diffuseColor) = if (submesh.name == "[player]") {
          val playerColor = Data.colors(playerId)
          val ambientColor = submesh.ambientColor.copy()
          val diffuseColor = submesh.diffuseColor.copy()
          def componentsMult(src: Vector3f, dst: Vector3f): Unit = {
            dst.x *= src.x
            dst.y *= src.y
            dst.z *= src.z
          }
          componentsMult(playerColor, ambientColor)
          componentsMult(playerColor, diffuseColor)
          (ambientColor, diffuseColor)
        } else (submesh.ambientColor, submesh.diffuseColor)
        gl.uniform3f(ambientColorUniLoc, ambientColor)
        gl.uniform3f(diffuseColorUniLoc, diffuseColor)
        gl.bindBuffer(GLES2.ELEMENT_ARRAY_BUFFER, submesh.indicesBuffer)
        gl.drawElements(GLES2.TRIANGLES, submesh.verticesCount, GLES2.UNSIGNED_SHORT, 0)
      }
    }
  }

  object Hud {
    var program: Token.Program = _

    var sightVerticesBuffer: Token.Buffer = _
    var sightIndicesBuffer: Token.Buffer = _

    var healthVerticesBuffer: Token.Buffer = _
    var healthIndicesBuffer: Token.Buffer = _

    var sightRenderCount: Int = _
    var healthRenderCount: Int = _

    var positionAttrLoc: Int = _

    var colorUniLoc: Token.UniformLocation = _
    var transformUniLoc: Token.UniformLocation = _
    var scaleXUniLoc: Token.UniformLocation = _
    var scaleYUniLoc: Token.UniformLocation = _

    var sightTransforms: Array[Matrix3f] = _

    def setup(program: Token.Program)(implicit gl: GLES2): Unit = {
      this.program = program

      positionAttrLoc = gl.getAttribLocation(program, "position")

      colorUniLoc = gl.getUniformLocation(program, "color")
      transformUniLoc = gl.getUniformLocation(program, "transform")
      scaleXUniLoc = gl.getUniformLocation(program, "scaleX")
      scaleYUniLoc = gl.getUniformLocation(program, "scaleY")

      // Sight

      val sightVerticesData = GLES2.createFloatBuffer(3 * 2) // 3 vertices, 2 coordinates each
      sightVerticesData.put(0f).put(0f)
      sightVerticesData.put(0.025f).put(0.1f)
      sightVerticesData.put(-0.025f).put(0.1f)
      sightVerticesData.flip()

      this.sightVerticesBuffer = gl.createBuffer()
      gl.bindBuffer(GLES2.ARRAY_BUFFER, sightVerticesBuffer)
      gl.bufferData(GLES2.ARRAY_BUFFER, sightVerticesData, GLES2.STATIC_DRAW)

      this.sightRenderCount = 3
      val sightIndicesData = GLES2.createShortBuffer(sightRenderCount)
      sightIndicesData.put(0.toShort)
      sightIndicesData.put(1.toShort)
      sightIndicesData.put(2.toShort)
      sightIndicesData.flip()

      this.sightIndicesBuffer = gl.createBuffer()
      gl.bindBuffer(GLES2.ELEMENT_ARRAY_BUFFER, sightIndicesBuffer)
      gl.bufferData(GLES2.ELEMENT_ARRAY_BUFFER, sightIndicesData, GLES2.STATIC_DRAW)

      val baseTransform = Matrix3f.translate2D(new Vector2f(0f, 0.05f))

      this.sightTransforms = new Array(3)
      this.sightTransforms(0) = Matrix3f.rotate2D(0f) * baseTransform
      this.sightTransforms(1) = Matrix3f.rotate2D(120f) * baseTransform
      this.sightTransforms(2) = Matrix3f.rotate2D(240f) * baseTransform

      // Health indicator

      val healthVerticesData = GLES2.createFloatBuffer(4 * 2) // 4 vertices, 2 coordinates each
      healthVerticesData.put(-1f).put(1f)
      healthVerticesData.put(-1f).put(0.95f)
      healthVerticesData.put(+1f).put(1f)
      healthVerticesData.put(+1f).put(0.95f)
      healthVerticesData.flip()

      this.healthVerticesBuffer = gl.createBuffer()
      gl.bindBuffer(GLES2.ARRAY_BUFFER, healthVerticesBuffer)
      gl.bufferData(GLES2.ARRAY_BUFFER, healthVerticesData, GLES2.STATIC_DRAW)

      this.healthRenderCount = 6
      val healthIndicesData = GLES2.createShortBuffer(healthRenderCount)
      healthIndicesData.put(0.toShort)
      healthIndicesData.put(1.toShort)
      healthIndicesData.put(2.toShort)
      healthIndicesData.put(1.toShort)
      healthIndicesData.put(2.toShort)
      healthIndicesData.put(3.toShort)
      healthIndicesData.flip()

      this.healthIndicesBuffer = gl.createBuffer()
      gl.bindBuffer(GLES2.ELEMENT_ARRAY_BUFFER, healthIndicesBuffer)
      gl.bufferData(GLES2.ELEMENT_ARRAY_BUFFER, healthIndicesData, GLES2.STATIC_DRAW)

      gl.checkError()
    }

    def init()(implicit gl: GLES2): Unit = {
      gl.useProgram(program)

      gl.enableVertexAttribArray(positionAttrLoc)
    }

    def close()(implicit gl: GLES2): Unit = {
      gl.disableVertexAttribArray(positionAttrLoc)
    }

    def render(playerId: Int, screenWidth: Int, screenHeight: Int, health: Float, timeSinceLastTime: Option[Int])(implicit gl: GLES2): Unit = {
      val screenRatio = screenHeight.toFloat / screenWidth.toFloat
      val screenRatioInv = screenWidth.toFloat / screenHeight.toFloat

      val timeForHitNotificationMs = 200

      // Sight
      gl.uniform1f(scaleXUniLoc, screenRatio)
      gl.uniform1f(scaleYUniLoc, 1f)

      gl.uniform3f(colorUniLoc, Data.colors(playerId))

      gl.bindBuffer(GLES2.ARRAY_BUFFER, this.sightVerticesBuffer)
      gl.vertexAttribPointer(positionAttrLoc, 2, GLES2.FLOAT, false, 0, 0)

      gl.bindBuffer(GLES2.ELEMENT_ARRAY_BUFFER, this.sightIndicesBuffer)

      val extraTransform = timeSinceLastTime match { // Hit notification
        case Some(time) if (time < timeForHitNotificationMs) => Matrix3f.translate2D(new Vector2f(0f, Physics.interpol(time, 0, timeForHitNotificationMs, 0.1f, 0f)))
        case _ => new Matrix3f
      }

      for (transform <- this.sightTransforms) {
        gl.uniformMatrix3f(transformUniLoc, transform * extraTransform)
        gl.drawElements(GLES2.TRIANGLES, this.sightRenderCount, GLES2.UNSIGNED_SHORT, 0)
      }

      // Health
      gl.uniform1f(scaleXUniLoc, 1f)
      gl.uniform1f(scaleYUniLoc, 1f)

      val healthRatio = health / 100f

      val healthColor = new Vector3f(1f - healthRatio, healthRatio, 0f)
      val healthTransform = Matrix3f.scale3D(new Vector3f(healthRatio, 1f, 1f))

      gl.uniform3f(colorUniLoc, healthColor)

      gl.bindBuffer(GLES2.ARRAY_BUFFER, this.healthVerticesBuffer)
      gl.vertexAttribPointer(positionAttrLoc, 2, GLES2.FLOAT, false, 0, 0)

      gl.bindBuffer(GLES2.ELEMENT_ARRAY_BUFFER, this.healthIndicesBuffer)

      gl.uniformMatrix3f(transformUniLoc, healthTransform)
      gl.drawElements(GLES2.TRIANGLES, this.healthRenderCount, GLES2.UNSIGNED_SHORT, 0)
    }
  }
}
