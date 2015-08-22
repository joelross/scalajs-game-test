package games.utils

import scala.collection.mutable.Map
import scala.collection.mutable.ArrayBuffer

import games.math.{ Vector2f, Vector3f, Vector4f, Matrix3f }

object SimpleOBJParser {
  class TexInfo(var path: String) {
    var blendu: Boolean = true
    var blendv: Boolean = true
    var bumpMultiplier: Option[Float] = None
    var boost: Option[Float] = None
    var colorCorrection: Option[Boolean] = None
    var clamp: Boolean = false
    var channel: Option[String] = None
    var modifierBase: Float = 0f
    var modifierGain: Float = 1f
    var offset: Vector3f = new Vector3f(0, 0, 0)
    var resize: Vector3f = new Vector3f(1, 1, 1)
    var turbulence: Vector3f = new Vector3f(0, 0, 0)
    var resolution: Option[Int] = None

    override def toString(): String = "TexInfo(path=\"" + path + "\")"
  }

  class Material(val name: String) {
    var ambientColor: Option[Vector3f] = None
    var diffuseColor: Option[Vector3f] = None
    var specularColor: Option[Vector3f] = None
    var specularCoef: Option[Float] = None
    var sharpness: Float = 60f
    var refractionIndex: Option[Float] = None
    var transparency: Option[Float] = None
    var illuminationModelIndex: Option[Int] = None
    var ambientColorTexture: Option[TexInfo] = None
    var diffuseColorTexture: Option[TexInfo] = None
    var specularColorTexture: Option[TexInfo] = None
    var specularCoefTexture: Option[TexInfo] = None
    var bumpMapTexture: Option[TexInfo] = None
    var displacementMapTexture: Option[TexInfo] = None
    var decalTexture: Option[TexInfo] = None

    override def toString(): String = "Material(name=\"" + name + "\")"
  }

  private def onOff(value: String): Boolean = value.toLowerCase() match {
    case "1" | "on"  => true
    case "0" | "off" => false
    case _           => throw new RuntimeException("Unknown value \"" + value + "\"")
  }

  // From http://en.wikipedia.org/wiki/CIE_1931_color_space#Construction_of_the_CIE_XYZ_color_space_from_the_Wright.E2.80.93Guild_data
  private val cieToRgbMatrix = new Matrix3f(
    0.41847f, -0.15866f, -0.082835f,
    -0.091169f, 0.25243f, 0.015708f,
    0.00092090f, -0.0025498f, 0.17860f)

  private def cieToRgb(cie: Vector3f): Vector3f = cieToRgbMatrix * cie

  private def parseFloat(s: String) = try { Some(s.toFloat) } catch { case _: Throwable => None }

  private def parseTex(tokens: Array[String]): TexInfo = {
    val texInfo = new TexInfo("<undefined>")

    var currentShift = 1 // First token is the command name

    while (currentShift < tokens.size) {
      val remaining = tokens.size - currentShift + 1

      tokens(currentShift).toLowerCase() match {
        case "-blendu" if (remaining >= 2) =>
          texInfo.blendu = onOff(tokens(currentShift + 1))
          currentShift += 2

        case "-blendv" if (remaining >= 2) =>
          texInfo.blendv = onOff(tokens(currentShift + 1))
          currentShift += 2

        case "-bm" if (remaining >= 2) =>
          texInfo.bumpMultiplier = Some(tokens(currentShift + 1).toFloat)
          currentShift += 2

        case "-boost" if (remaining >= 2) =>
          texInfo.boost = Some(tokens(currentShift + 1).toFloat)
          currentShift += 2

        case "-cc" if (remaining >= 2) =>
          texInfo.colorCorrection = Some(onOff(tokens(currentShift + 1)))
          currentShift += 2

        case "-clamp" if (remaining >= 2) =>
          texInfo.clamp = onOff(tokens(currentShift + 1))
          currentShift += 2

        case "-imfchan" if (remaining >= 2) =>
          texInfo.channel = Some(tokens(currentShift + 1).toLowerCase())
          currentShift += 2

        case "-mm" if (remaining >= 3) =>
          texInfo.modifierBase = tokens(currentShift + 1).toFloat
          texInfo.modifierGain = tokens(currentShift + 2).toFloat
          currentShift += 3

        case "-o" if (remaining >= 2) =>
          val x = tokens(currentShift + 1).toFloat

          (if (remaining >= 3) parseFloat(tokens(currentShift + 2)) else None) match {
            case None =>
              texInfo.offset = new Vector3f(x, 0, 0)
              currentShift += 2

            case Some(y) => (if (remaining >= 4) parseFloat(tokens(currentShift + 3)) else None) match {
              case None =>
                texInfo.offset = new Vector3f(x, y, 0)
                currentShift += 3

              case Some(z) =>
                texInfo.offset = new Vector3f(x, y, z)
                currentShift += 4
            }
          }

        case "-s" if (remaining >= 2) =>
          val x = tokens(currentShift + 1).toFloat

          (if (remaining >= 3) parseFloat(tokens(currentShift + 2)) else None) match {
            case None =>
              texInfo.resize = new Vector3f(x, 1, 1)
              currentShift += 2

            case Some(y) => (if (remaining >= 4) parseFloat(tokens(currentShift + 3)) else None) match {
              case None =>
                texInfo.resize = new Vector3f(x, y, 1)
                currentShift += 3

              case Some(z) =>
                texInfo.resize = new Vector3f(x, y, z)
                currentShift += 4
            }
          }

        case "-t" if (remaining >= 2) =>
          val x = tokens(currentShift + 1).toFloat

          (if (remaining >= 3) parseFloat(tokens(currentShift + 2)) else None) match {
            case None =>
              texInfo.turbulence = new Vector3f(x, 0, 0)
              currentShift += 2

            case Some(y) => (if (remaining >= 4) parseFloat(tokens(currentShift + 3)) else None) match {
              case None =>
                texInfo.turbulence = new Vector3f(x, y, 0)
                currentShift += 3

              case Some(z) =>
                texInfo.turbulence = new Vector3f(x, y, z)
                currentShift += 4
            }
          }

        case "-texres" if (remaining >= 2) =>
          texInfo.resolution = Some(tokens(currentShift + 1).toInt)
          currentShift += 2

        case _ =>
          texInfo.path = tokens(currentShift)
          currentShift += 1
      }
    }

    texInfo
  }

  def parseMTL(mtlFile: String): scala.collection.Map[String, Material] = {
    val mats: Map[String, Material] = Map()
    var curMat: Option[Material] = None

    def mat(): Material = curMat.getOrElse(throw new RuntimeException("No material currently selected"))

    def flushCurMat(): Unit = for (cur <- curMat) {
      mats += (cur.name -> cur)
      curMat = None
    }

    for (currentLine <- mtlFile.lines) {
      val index = currentLine.indexOf("#")
      val line = if (index < 0) currentLine else currentLine.substring(0, index).trim()

      val tokens = line.split(" ", -1)

      (tokens(0).toLowerCase(), if (tokens.size >= 2) Some(tokens(1).toLowerCase()) else None) match {
        case ("newmtl", _) if (tokens.size >= 2) =>
          flushCurMat()

          val matName = tokens(1)

          val newMat = new Material(matName)
          curMat = Some(newMat)

        case ("ka", Some("spectral")) => println("Spectral Ka not supported")

        case ("ka", Some("xyz")) if (tokens.size >= 5) =>
          val x = tokens(2).toFloat
          val y = tokens(3).toFloat
          val z = tokens(4).toFloat

          val cieXYZ = new Vector3f(x, y, z)
          mat().ambientColor = Some(cieToRgb(cieXYZ))

        case ("ka", _) if (tokens.size >= 4) =>
          val r = tokens(1).toFloat
          val g = tokens(2).toFloat
          val b = tokens(3).toFloat

          val rgb = new Vector3f(r, g, b)
          mat().ambientColor = Some(rgb)

        case ("kd", Some("spectral")) => println("Spectral Kd not supported")

        case ("kd", Some("xyz")) if (tokens.size >= 5) =>
          val x = tokens(2).toFloat
          val y = tokens(3).toFloat
          val z = tokens(4).toFloat

          val cieXYZ = new Vector3f(x, y, z)
          mat().diffuseColor = Some(cieToRgb(cieXYZ))

        case ("kd", _) if (tokens.size >= 4) =>
          val r = tokens(1).toFloat
          val g = tokens(2).toFloat
          val b = tokens(3).toFloat

          val rgb = new Vector3f(r, g, b)
          mat().diffuseColor = Some(rgb)

        case ("ks", Some("spectral")) => println("Spectral Ks not supported")

        case ("ks", Some("xyz")) if (tokens.size >= 5) =>
          val x = tokens(2).toFloat
          val y = tokens(3).toFloat
          val z = tokens(4).toFloat

          val cieXYZ = new Vector3f(x, y, z)
          mat().specularColor = Some(cieToRgb(cieXYZ))

        case ("ks", _) if (tokens.size >= 4) =>
          val r = tokens(1).toFloat
          val g = tokens(2).toFloat
          val b = tokens(3).toFloat

          val rgb = new Vector3f(r, g, b)
          mat().specularColor = Some(rgb)

        case ("tf", _) => println("Transmission filter not supported")

        case ("illum", _) if (tokens.size >= 2) =>
          val illum = tokens(1).toInt
          mat().illuminationModelIndex = Some(illum)

        case ("d", _) | ("tr", _) if (tokens.size >= 2) =>
          val tr = tokens(1).toFloat
          mat().transparency = Some(tr)

        case ("ns", _) if (tokens.size >= 2) =>
          val n = tokens(1).toFloat
          mat().specularCoef = Some(n)

        case ("sharpness", _) if (tokens.size >= 2) =>
          val sharp = tokens(1).toFloat
          mat().sharpness = sharp

        case ("ni", _) if (tokens.size >= 2) =>
          val indexOfRefraction = tokens(1).toFloat
          mat().refractionIndex = Some(indexOfRefraction)

        case ("map_ka", _) if (tokens.size >= 2) =>
          val texInfo = parseTex(tokens)
          mat().ambientColorTexture = Some(texInfo)

        case ("map_kd", _) if (tokens.size >= 2) =>
          val texInfo = parseTex(tokens)
          mat().diffuseColorTexture = Some(texInfo)

        case ("map_ks", _) if (tokens.size >= 2) =>
          val texInfo = parseTex(tokens)
          mat().specularColorTexture = Some(texInfo)

        case ("map_ns", _) if (tokens.size >= 2) =>
          val texInfo = parseTex(tokens)
          mat().specularCoefTexture = Some(texInfo)

        case ("", _)  => // Empty line (probably a comment), ignore
        case (arg, _) => println("Unknown or invalid MTL command \"" + arg + "\", ignoring the line")
      }
    }

    flushCurMat()

    mats
  }

  case class OBJVertex(position: Int, texture: Option[Int], normal: Option[Int])
  type OBJFace = Array[OBJVertex]

  class OBJObjectGroupPart(val material: Option[Material]) {
    val faces: ArrayBuffer[OBJFace] = new ArrayBuffer[OBJFace]()

    override def toString(): String = material match {
      case Some(mat) => "ObjectGroupPart(material=\"" + mat.name + "\")"
      case None      => "ObjectGroupPart(no material)"
    }
  }

  class OBJObjectGroup(val name: String) {
    var smooth: Boolean = false

    val parts: ArrayBuffer[OBJObjectGroupPart] = new ArrayBuffer[OBJObjectGroupPart]()

    override def toString(): String = "ObjectGroup(name=\"" + name + "\")"
  }

  class OBJObject(val name: String) {
    val vertices: ArrayBuffer[Vector4f] = new ArrayBuffer[Vector4f]()
    val texCoordinates: ArrayBuffer[Vector3f] = new ArrayBuffer[Vector3f]()
    val normals: ArrayBuffer[Vector3f] = new ArrayBuffer[Vector3f]()
    val parameterVertices: ArrayBuffer[Vector3f] = new ArrayBuffer[Vector3f]()

    val groups: ArrayBuffer[OBJObjectGroup] = ArrayBuffer[OBJObjectGroup]()

    override def toString(): String = "Object(name=\"" + name + "\")"
  }

  def parseOBJ(objFile: String, mtlFiles: scala.collection.Map[String, String]): scala.collection.Map[String, OBJObject] = {
    val objs: Map[String, OBJObject] = Map()

    var curObjGroupPart: Option[OBJObjectGroupPart] = None
    var curObjGroup: Option[OBJObjectGroup] = None
    var curObj: Option[OBJObject] = None

    val availableMats: Map[String, Material] = Map()

    def objGroupPart(): OBJObjectGroupPart = curObjGroupPart.getOrElse(throw new RuntimeException("No material currently selected for object"))

    def objGroup(): OBJObjectGroup = curObjGroup.getOrElse(throw new RuntimeException("No group currently selected for object"))

    def obj(): OBJObject = curObj.getOrElse(throw new RuntimeException("No object currently selected"))

    def flushCurObjGroupPart(): Unit = for (cur <- curObjGroupPart) {
      if (!objGroup().parts.contains(cur)) objGroup().parts += cur
      curObjGroupPart = None
    }

    def flushCurObjGroup(): Unit = for (cur <- curObjGroup) {
      flushCurObjGroupPart()

      if (!obj().groups.contains(cur)) obj().groups += cur
      curObjGroup = None
    }

    def flushCurObj(): Unit = for (cur <- curObj) {
      flushCurObjGroup()

      if (!objs.contains(cur.name)) objs += (cur.name -> cur)
      curObj = None
    }

    def getObjGroupPart(material: Option[Material]): OBJObjectGroupPart = {
      val existingPart = objGroup().parts.find { _.material == material }
      existingPart.getOrElse(new OBJObjectGroupPart(material))
    }

    def getObjGroup(name: String): OBJObjectGroup = {
      val existingGroup = obj().groups.find { _.name == name }
      existingGroup.getOrElse(new OBJObjectGroup(name))
    }

    def getObj(name: String): OBJObject = {
      val existingObj = objs.get(name)
      existingObj.getOrElse(new OBJObject(name))
    }

    for (currentLine <- objFile.lines) {
      val index = currentLine.indexOf("#")
      val line = if (index < 0) currentLine else currentLine.substring(0, index).trim()

      val tokens = line.split(" ", -1)

      tokens(0).toLowerCase() match {

        // Vertex data

        case "v" if (tokens.size >= 4) =>
          val x = tokens(1).toFloat
          val y = tokens(2).toFloat
          val z = tokens(3).toFloat
          val w = if (tokens.size >= 5) tokens(4).toFloat else 1.0f

          val pos = new Vector4f(x, y, z, w)
          obj().vertices += pos

        case "vp" if (tokens.size >= 2) =>
          val u = tokens(1).toFloat
          val v = if (tokens.size >= 3) tokens(2).toFloat else 1.0f
          val w = if (tokens.size >= 4) tokens(3).toFloat else 1.0f

          val param = new Vector3f(u, v, w)
          obj().parameterVertices += param

        case "vn" if (tokens.size >= 4) =>
          val x = tokens(1).toFloat
          val y = tokens(2).toFloat
          val z = tokens(3).toFloat

          val norm = new Vector3f(x, y, z)
          obj().normals += norm

        case "vt" if (tokens.size >= 2) =>
          val u = tokens(1).toFloat
          val v = if (tokens.size >= 3) tokens(2).toFloat else 0.0f
          val w = if (tokens.size >= 4) tokens(3).toFloat else 0.0f

          val coord = new Vector3f(u, v, w)
          obj().texCoordinates += coord

        // Free-form curve/surface attributes

        case "cstype" => println("Type of curve not supported")

        case "deg"    => println("Degree for curves and surfaces not supported")

        case "bmat"   => println("Basis matrices not supported")

        case "step"   => println("Step size for surces and surfaces not supported")

        // Elements

        case "p"      => println("Point element not supported")

        case "l"      => println("Line element not supported")

        case "f" =>
          val face = new Array[OBJVertex](tokens.size - 1)

          def strToInt(str: String): Option[Int] = {
            if (str == "") None
            else Some(str.toInt)
          }

          for (currentToken <- 1 until tokens.size) {
            val indices = tokens(currentToken).split("/")

            val vertex = indices.length match {
              case 1 => OBJVertex(indices(0).toInt, None, None)
              case 2 => OBJVertex(indices(0).toInt, strToInt(indices(1)), None)
              case 3 => OBJVertex(indices(0).toInt, strToInt(indices(1)), strToInt(indices(2)))
              case _ => throw new RuntimeException("Malformed vertex data \"" + tokens(currentToken) + "\"")
            }

            face(currentToken - 1) = vertex
          }

          objGroupPart().faces += face

        case "curv"  => println("Curve element not supported")

        case "curv2" => println("2D curve element not supported")

        case "surf"  => println("Surface element not supported")

        // Special curve and point

        case "parm"  => println("Parameter not supported")

        case "trim"  => println("Trimming not supported")

        case "hole"  => println("Hole not supported")

        case "scrv"  => println("Curve sequence not supported")

        case "sp"    => println("Special point not supported")

        case "end"   => println("End not supported")

        // Connectivity

        case "con"   => println("Connectivity not supported")

        // Grouping

        case "g" if (tokens.size >= 2) =>
          flushCurObjGroup()

          val groupName = tokens(1)
          val newObjGroup = getObjGroup(groupName)
          curObjGroup = Some(newObjGroup)

          val newObjGroupPart = getObjGroupPart(None)
          curObjGroupPart = Some(newObjGroupPart)

        case "s" if (tokens.size >= 2) =>
          val smooth = onOff(tokens(1))
          objGroup().smooth = smooth

        case "mg" => println("Merging group not supported")

        case "o" if (tokens.size >= 2) =>
          flushCurObj()

          val objName = tokens(1)
          val newObj = getObj(objName)
          curObj = Some(newObj)

          val newObjGroup = getObjGroup("default")
          curObjGroup = Some(newObjGroup)

          val newObjGroupPart = getObjGroupPart(None)
          curObjGroupPart = Some(newObjGroupPart)

        // Display/render attributes

        case "bevel"    => println("Bevel not supported")

        case "c_interp" => println("Color interopolation not supported")

        case "d_interp" => println("Dissolve interpolation not supported")

        case "lod"      => println("Level of detail not supported")

        case "maplib"   => println("Library mapping not supported")

        case "usemap"   => println("Use mapping not supported")

        case "usemtl" if (tokens.size >= 2) =>
          flushCurObjGroupPart()

          val selectedMatName = tokens(1)
          val selectedMat = availableMats(selectedMatName)
          val newSubObj = getObjGroupPart(Some(selectedMat))
          curObjGroupPart = Some(newSubObj)

        case "mtllib" if (tokens.size >= 2) =>
          val mtlFile = mtlFiles(tokens(1))

          availableMats ++= parseMTL(mtlFile)

        case "shadow_obj" => println("Shadow object not supported")

        case "trace_obj"  => println("Tracing object not supported")

        case "ctech"      => println("Curve approximation not supported")

        case "stech"      => println("Surface approximation not supported")

        // Curve and surface operation

        case "bsp"        => println("B-spline patch not supported")

        case "bzp"        => println("Bezier patch not supported")

        case "cdc"        => println("Cardinal curve not supported")

        case "cdp"        => println("Cardinal patch not supported")

        case "res"        => println("Reference and display not supported")

        // Misc

        case ""           => // Empty line (probably a comment), ignore
        case arg          => println("Unknown or invalid OBJ command \"" + arg + "\", ignoring the line")
      }
    }

    flushCurObj()

    objs
  }

  case class VertexData(position: Vector3f, texture: Option[Vector2f], normal: Option[Vector3f])
  type Tri = (Int, Int, Int) // The three indices of the vertices of the triangle

  class SubTriMesh(val material: Option[Material], val tris: Array[Tri]) {
    override def toString(): String = material.fold { "SubTriMesh(no material)" } { mat => s"""SubTriMesh(material="${mat.name}")""" }
  }

  class TriMesh(val name: String, val vertices: Array[Vector3f], val texCoordinates: Option[Array[Vector2f]],
                val normals: Option[Array[Vector3f]], val submeshes: Array[SubTriMesh]) {
    override def toString(): String = """TriMesh(name="${name}")"""
  }

  def convOBJObjectToTriMesh(obj: OBJObject): TriMesh = {
    val subs = new ArrayBuffer[SubTriMesh]()

    val vertices = new ArrayBuffer[Vector3f]()
    val texCoordinates = new ArrayBuffer[Vector2f]()
    val normals = new ArrayBuffer[Vector3f]()

    def bufferIndexOfVertex(vertexData: VertexData): Int = {
      val vertex = vertexData.position
      val texCoordinate = vertexData.texture
      val normal = vertexData.normal

      val index = (texCoordinate, normal) match {
        case (Some(tex), Some(norm)) =>
          for (i <- 0 until vertices.size) {
            if (vertices(i) == vertex && texCoordinates(i) == tex && normals(i) == norm) return i
          }
          // No matching vertex data found, add it at the end
          vertices += vertex
          texCoordinates += tex
          normals += norm

          vertices.size - 1 // return index of the new vertex

        case (None, Some(norm)) =>
          for (i <- 0 until vertices.size) {
            if (vertices(i) == vertex && normals(i) == norm) return i
          }

          // No matching vertex data found, add it at the end
          vertices += vertex
          normals += norm

          vertices.size - 1 // return index of the new vertex

        case (Some(tex), None) =>
          for (i <- 0 until vertices.size) {
            if (vertices(i) == vertex && texCoordinates(i) == tex) return i
          }

          // No matching vertex data found, add it at the end
          vertices += vertex
          texCoordinates += tex

          vertices.size - 1 // return index of the new vertex

        case (None, None) =>
          for (i <- 0 until vertices.size) {
            if (vertices(i) == vertex) return i
          }

          // No matching vertex data found, add it at the end
          vertices += vertex

          vertices.size - 1 // return index of the new vertex
      }

      val formatErr = "The vertex data format is not uniform accross the vertices"
      if (texCoordinates.size > 0 && texCoordinates.size != vertices.size) throw new RuntimeException(formatErr)
      if (normals.size > 0 && normals.size != vertices.size) throw new RuntimeException(formatErr)

      index
    }

    obj.groups.foreach { group =>
      group.parts.filter { _.faces.size > 0 }.foreach { part =>
        val trisIndices = new ArrayBuffer[Tri]()

        def addTri(v0: OBJVertex, v1: OBJVertex, v2: OBJVertex): Unit = {
          def dataFromFileIndices(v: OBJVertex): VertexData = {
            // Data in OBJ files are indexed from 1 (instead of 0)
            val indexV = v.position - 1
            val optIndexT = v.texture.map{_ - 1}
            val optIndexN = v.normal.map{_ - 1}

            val ova = obj.vertices(indexV)
            val ov = new Vector3f(ova.x, ova.y, ova.z)
            val ot = optIndexT.map { t => val ota = obj.texCoordinates(t); new Vector2f(ota.x, ota.y) }
            val on = optIndexN.map { n => obj.normals(n) }

            VertexData(ov, ot, on)
          }

          val v0Data = dataFromFileIndices(v0)
          val v1Data = dataFromFileIndices(v1)
          val v2Data = dataFromFileIndices(v2)

          val v0Index = bufferIndexOfVertex(v0Data)
          val v1Index = bufferIndexOfVertex(v1Data)
          val v2Index = bufferIndexOfVertex(v2Data)

          val newTri: Tri = (v0Index, v1Index, v2Index)
          trisIndices += newTri
        }

        part.faces.foreach { face =>
          face.size match {
            case 3 =>
              val v0 = face(0)
              val v1 = face(1)
              val v2 = face(2)

              addTri(v0, v1, v2)

            case 4 =>
              val v0 = face(0)
              val v1 = face(1)
              val v2 = face(2)
              val v3 = face(3)

              addTri(v0, v1, v3)
              addTri(v1, v2, v3)

            case _ => throw new RuntimeException("Only faces composed of 3 of 4 vertices are supported")
          }
        }

        val newSub = new SubTriMesh(part.material, trisIndices.toArray)
        subs += newSub
      }
    }

    new TriMesh(obj.name, vertices.toArray, if (texCoordinates.size > 0) Some(texCoordinates.toArray) else None, if (normals.size > 0) Some(normals.toArray) else None, subs.toArray)
  }
}
