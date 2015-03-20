package games.utils

import scala.collection.mutable.Map
import scala.collection.mutable.ArrayBuffer

import games.math.{ Vector2f, Vector3f, Vector4f, Matrix3f }
import games.opengl.GLException

object SimpleOBJParser {
  case class TexInfo(var path: String) {
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
  }

  case class Material(name: String) {
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

  private def onOff(value: String): Boolean = value match {
    case "1" | "on"  => true
    case "0" | "off" => false
    case _           => true
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
        case "-blendu" if (remaining >= 2) => {
          texInfo.blendu = onOff(tokens(currentShift + 1))
          currentShift += 2
        }

        case "-blendv" if (remaining >= 2) => {
          texInfo.blendv = onOff(tokens(currentShift + 1))
          currentShift += 2
        }

        case "-bm" if (remaining >= 2) => {
          texInfo.bumpMultiplier = Some(tokens(currentShift + 1).toFloat)
          currentShift += 2
        }

        case "-boost" if (remaining >= 2) => {
          texInfo.boost = Some(tokens(currentShift + 1).toFloat)
          currentShift += 2
        }

        case "-cc" if (remaining >= 2) => {
          texInfo.colorCorrection = Some(onOff(tokens(currentShift + 1)))
          currentShift += 2
        }

        case "-clamp" if (remaining >= 2) => {
          texInfo.clamp = onOff(tokens(currentShift + 1))
          currentShift += 2
        }

        case "-imfchan" if (remaining >= 2) => {
          texInfo.channel = Some(tokens(currentShift + 1).toLowerCase())
          currentShift += 2
        }

        case "-mm" if (remaining >= 3) => {
          texInfo.modifierBase = tokens(currentShift + 1).toFloat
          texInfo.modifierGain = tokens(currentShift + 2).toFloat
          currentShift += 3
        }

        case "-o" if (remaining >= 2) => {
          val x = tokens(currentShift + 1).toFloat

          (if (remaining >= 3) parseFloat(tokens(currentShift + 2)) else None) match {
            case None => {
              texInfo.offset = new Vector3f(x, 0, 0)
              currentShift += 2
            }
            case Some(y) => (if (remaining >= 4) parseFloat(tokens(currentShift + 3)) else None) match {
              case None => {
                texInfo.offset = new Vector3f(x, y, 0)
                currentShift += 3
              }
              case Some(z) => {
                texInfo.offset = new Vector3f(x, y, z)
                currentShift += 4
              }
            }
          }
        }

        case "-s" if (remaining >= 2) => {
          val x = tokens(currentShift + 1).toFloat

          (if (remaining >= 3) parseFloat(tokens(currentShift + 2)) else None) match {
            case None => {
              texInfo.resize = new Vector3f(x, 1, 1)
              currentShift += 2
            }
            case Some(y) => (if (remaining >= 4) parseFloat(tokens(currentShift + 3)) else None) match {
              case None => {
                texInfo.resize = new Vector3f(x, y, 1)
                currentShift += 3
              }
              case Some(z) => {
                texInfo.resize = new Vector3f(x, y, z)
                currentShift += 4
              }
            }
          }
        }

        case "-t" if (remaining >= 2) => {
          val x = tokens(currentShift + 1).toFloat

          (if (remaining >= 3) parseFloat(tokens(currentShift + 2)) else None) match {
            case None => {
              texInfo.turbulence = new Vector3f(x, 0, 0)
              currentShift += 2
            }
            case Some(y) => (if (remaining >= 4) parseFloat(tokens(currentShift + 3)) else None) match {
              case None => {
                texInfo.turbulence = new Vector3f(x, y, 0)
                currentShift += 3
              }
              case Some(z) => {
                texInfo.turbulence = new Vector3f(x, y, z)
                currentShift += 4
              }
            }
          }
        }

        case "-texres" if (remaining >= 2) => {
          texInfo.resolution = Some(tokens(currentShift + 1).toInt)
          currentShift += 2
        }

        case _ => {
          texInfo.path = tokens(currentShift)
          currentShift += 1
        }
      }
    }

    texInfo
  }

  def parseMTL(mtlFile: Array[String]): scala.collection.Map[String, Material] = {
    val mats: Map[String, Material] = Map()
    var curMat: Option[Material] = None

    def mat(): Material = curMat match {
      case Some(cur) => cur
      case None      => throw new GLException("No material currently selected")
    }

    def flushCurMat(): Unit = curMat match {
      case Some(cur) => {
        mats += (cur.name -> cur)
        curMat = None
      }
      case None =>
    }

    for (currentLine <- mtlFile) {
      val index = currentLine.indexOf("#")
      val line = if (index < 0) currentLine else currentLine.substring(0, index).trim()

      val tokens = line.split(" ", -1)

      (tokens(0).toLowerCase(), if (tokens.size >= 2) Some(tokens(1).toLowerCase()) else None) match {
        case ("newmtl", _) if (tokens.size >= 2) => {
          flushCurMat()

          val matName = tokens(1)

          val newMat = new Material(matName)
          curMat = Some(newMat)
        }

        case ("ka", Some("spectral")) => throw new RuntimeException("Spectral Ka not supported")

        case ("ka", Some("xyz")) if (tokens.size >= 5) => {
          val x = tokens(2).toFloat
          val y = tokens(3).toFloat
          val z = tokens(4).toFloat

          val cieXYZ = new Vector3f(x, y, z)
          mat().ambientColor = Some(cieToRgb(cieXYZ))
        }

        case ("ka", _) if (tokens.size >= 4) => {
          val r = tokens(1).toFloat
          val g = tokens(2).toFloat
          val b = tokens(3).toFloat

          val rgb = new Vector3f(r, g, b)
          mat().ambientColor = Some(rgb)
        }

        case ("kd", Some("spectral")) => throw new RuntimeException("Spectral Kd not supported")

        case ("kd", Some("xyz")) if (tokens.size >= 5) => {
          val x = tokens(2).toFloat
          val y = tokens(3).toFloat
          val z = tokens(4).toFloat

          val cieXYZ = new Vector3f(x, y, z)
          mat().diffuseColor = Some(cieToRgb(cieXYZ))
        }

        case ("kd", _) if (tokens.size >= 4) => {
          val r = tokens(1).toFloat
          val g = tokens(2).toFloat
          val b = tokens(3).toFloat

          val rgb = new Vector3f(r, g, b)
          mat().diffuseColor = Some(rgb)
        }

        case ("ks", Some("spectral")) => throw new RuntimeException("Spectral Ks not supported")

        case ("ks", Some("xyz")) if (tokens.size >= 5) => {
          val x = tokens(2).toFloat
          val y = tokens(3).toFloat
          val z = tokens(4).toFloat

          val cieXYZ = new Vector3f(x, y, z)
          mat().specularColor = Some(cieToRgb(cieXYZ))
        }

        case ("ks", _) if (tokens.size >= 4) => {
          val r = tokens(1).toFloat
          val g = tokens(2).toFloat
          val b = tokens(3).toFloat

          val rgb = new Vector3f(r, g, b)
          mat().specularColor = Some(rgb)
        }

        case ("tf", _) => throw new RuntimeException("Transmission filter not supported")

        case ("illum", _) if (tokens.size >= 2) => {
          val illum = tokens(1).toInt
          mat().illuminationModelIndex = Some(illum)
        }

        case ("d", _) | ("tr", _) if (tokens.size >= 2) => {
          val tr = tokens(1).toFloat
          mat().transparency = Some(tr)
        }

        case ("ns", _) if (tokens.size >= 2) => {
          val n = tokens(1).toFloat
          mat().specularCoef = Some(n)
        }

        case ("sharpness", _) if (tokens.size >= 2) => {
          val sharp = tokens(1).toFloat
          mat().sharpness = sharp
        }

        case ("ni", _) if (tokens.size >= 2) => {
          val indexOfRefraction = tokens(1).toFloat
          mat().refractionIndex = Some(indexOfRefraction)
        }

        case ("map_ka", _) if (tokens.size >= 2) => {
          val texInfo = parseTex(tokens)
          mat().ambientColorTexture = Some(texInfo)
        }

        case ("map_kd", _) if (tokens.size >= 2) => {
          val texInfo = parseTex(tokens)
          mat().diffuseColorTexture = Some(texInfo)
        }

        case ("map_ks", _) if (tokens.size >= 2) => {
          val texInfo = parseTex(tokens)
          mat().specularColorTexture = Some(texInfo)
        }

        case ("map_ns", _) if (tokens.size >= 2) => {
          val texInfo = parseTex(tokens)
          mat().specularCoefTexture = Some(texInfo)
        }

        case ("", _)  => // Empty line (probably a comment), ignore
        case (arg, _) => throw new RuntimeException("Unknown or invalid MTL command \"" + arg + "\", ignoring the line")
      }
    }

    flushCurMat()

    mats
  }

  type TmpVertex = (Int, Option[Int], Option[Int]) // position index, texture index, normal index
  type TmpFace = Array[TmpVertex]

  case class OBJObjectGroupPart(material: Option[Material]) {
    val faces: ArrayBuffer[TmpFace] = new ArrayBuffer[TmpFace]()

    override def toString(): String = material match {
      case Some(mat) => "ObjectGroupPart(material=\"" + mat.name + "\")"
      case None      => "ObjectGroupPart(no material)"
    }
  }

  case class OBJObjectGroup(name: String) {
    var smooth: Boolean = false

    val parts: ArrayBuffer[OBJObjectGroupPart] = new ArrayBuffer[OBJObjectGroupPart]()

    override def toString(): String = "ObjectGroup(name=\"" + name + "\")"
  }

  case class OBJObject(name: String) {
    val vertices: ArrayBuffer[Vector4f] = new ArrayBuffer[Vector4f]()
    val texCoordinates: ArrayBuffer[Vector3f] = new ArrayBuffer[Vector3f]()
    val normals: ArrayBuffer[Vector3f] = new ArrayBuffer[Vector3f]()
    val parameterVertices: ArrayBuffer[Vector3f] = new ArrayBuffer[Vector3f]()

    val groups: ArrayBuffer[OBJObjectGroup] = ArrayBuffer[OBJObjectGroup]()

    override def toString(): String = "Object(name=\"" + name + "\")"
  }

  def parseOBJ(objFile: Array[String], extraFiles: scala.collection.Map[String, Array[String]]): scala.collection.Map[String, OBJObject] = {
    val objs: Map[String, OBJObject] = Map()

    var curObjGroupPart: Option[OBJObjectGroupPart] = None
    var curObjGroup: Option[OBJObjectGroup] = None
    var curObj: Option[OBJObject] = None

    val availableMats: Map[String, Material] = Map()

    def objGroupPart(): OBJObjectGroupPart = curObjGroupPart match {
      case Some(cur) => cur
      case None      => throw new GLException("No material currently selected for object")
    }

    def objGroup(): OBJObjectGroup = curObjGroup match {
      case Some(cur) => cur
      case None      => throw new GLException("No group currently selected for object")
    }

    def obj(): OBJObject = curObj match {
      case Some(cur) => cur
      case None      => throw new GLException("No object currently selected")
    }

    def flushCurObjGroupPart(): Unit = curObjGroupPart match {
      case Some(cur) => {
        if (!objGroup().parts.contains(cur)) objGroup().parts += cur
        curObjGroupPart = None
      }
      case None =>
    }

    def flushCurObjGroup(): Unit = curObjGroup match {
      case Some(cur) => {
        flushCurObjGroupPart()

        if (!obj().groups.contains(cur)) obj().groups += cur
        curObjGroup = None
      }
      case None =>
    }

    def flushCurObj(): Unit = curObj match {
      case Some(cur) => {
        flushCurObjGroup()

        if (!objs.contains(cur.name)) objs += (cur.name -> cur)
        curObj = None
      }
      case None =>
    }

    def getObjGroupPart(material: Option[Material]): OBJObjectGroupPart = {
      val existingPart = objGroup().parts.find { _.material == material }

      existingPart match {
        case Some(part) => part
        case None       => new OBJObjectGroupPart(material)
      }
    }

    def getObjGroup(name: String): OBJObjectGroup = {
      val existingGroup = obj().groups.find { _.name == name }

      existingGroup match {
        case Some(group) => group
        case None        => new OBJObjectGroup(name)
      }
    }

    def getObj(name: String): OBJObject = {
      val existingObj = objs.get(name)

      existingObj match {
        case Some(obj) => obj
        case None      => new OBJObject(name)
      }
    }

    for (currentLine <- objFile) {
      val index = currentLine.indexOf("#")
      val line = if (index < 0) currentLine else currentLine.substring(0, index).trim()

      val tokens = line.split(" ", -1)

      tokens(0).toLowerCase() match {

        // Vertex data

        case "v" if (tokens.size >= 4) => {
          val x = tokens(1).toFloat
          val y = tokens(2).toFloat
          val z = tokens(3).toFloat
          val w = if (tokens.size >= 5) tokens(4).toFloat else 1.0f

          val pos = new Vector4f(x, y, z, w)
          obj().vertices += pos
        }

        case "vp" if (tokens.size >= 2) => {
          val u = tokens(1).toFloat
          val v = if (tokens.size >= 3) tokens(2).toFloat else 1.0f
          val w = if (tokens.size >= 4) tokens(3).toFloat else 1.0f

          val param = new Vector3f(u, v, w)
          obj().parameterVertices += param
        }

        case "vn" if (tokens.size >= 4) => {
          val x = tokens(1).toFloat
          val y = tokens(2).toFloat
          val z = tokens(3).toFloat

          val norm = new Vector3f(x, y, z)
          obj().normals += norm
        }

        case "vt" if (tokens.size >= 2) => {
          val u = tokens(1).toFloat
          val v = if (tokens.size >= 3) tokens(2).toFloat else 0.0f
          val w = if (tokens.size >= 4) tokens(3).toFloat else 0.0f

          val coord = new Vector3f(u, v, w)
          obj().texCoordinates += coord
        }

        // Free-form curve/surface attributes

        case "cstype" => throw new RuntimeException("Type of curve not supported")

        case "deg"    => throw new RuntimeException("Degree for curves and surfaces not supported")

        case "bmat"   => throw new RuntimeException("Basis matrices not supported")

        case "step"   => throw new RuntimeException("Step size for surces and surfaces not supported")

        // Elements

        case "p"      => throw new RuntimeException("Point element not supported")

        case "l"      => throw new RuntimeException("Line element not supported")

        case "f" => {
          val face = new Array[TmpVertex](tokens.size - 1)

          def strToInt(str: String): Option[Int] = {
            if (str == "") None
            else Some(str.toInt)
          }

          var currentToken = 1
          while (currentToken < tokens.size) {
            val indices = tokens(currentToken).split("/")

            val vertex: TmpVertex = indices.length match {
              case 1 => (indices(0).toInt, None, None)
              case 2 => (indices(0).toInt, strToInt(indices(1)), None)
              case 3 => (indices(0).toInt, strToInt(indices(1)), strToInt(indices(2)))
              case _ => throw new GLException("Malformed vertex data \"" + tokens(currentToken) + "\"")
            }

            face(currentToken - 1) = vertex

            currentToken += 1
          }

          objGroupPart().faces += face
        }

        case "curv"  => throw new RuntimeException("Curve element not supported")

        case "curv2" => throw new RuntimeException("2D curve element not supported")

        case "surf"  => throw new RuntimeException("Surface element not supported")

        // Special curve and point

        case "parm"  => throw new RuntimeException("Parameter not supported")

        case "trim"  => throw new RuntimeException("Trimming not supported")

        case "hole"  => throw new RuntimeException("Hole not supported")

        case "scrv"  => throw new RuntimeException("Curve sequence not supported")

        case "sp"    => throw new RuntimeException("Special point not supported")

        case "end"   => throw new RuntimeException("End not supported")

        // Connectivity

        case "con"   => throw new RuntimeException("Connectivity not supported")

        // Grouping

        case "g" if (tokens.size >= 2) => {
          flushCurObjGroup()

          val groupName = tokens(1)
          val newObjGroup = getObjGroup(groupName)
          curObjGroup = Some(newObjGroup)

          val newObjGroupPart = getObjGroupPart(None)
          curObjGroupPart = Some(newObjGroupPart)
        }

        case "s" if (tokens.size >= 2) => {
          val smooth = onOff(tokens(1))
          objGroup().smooth = smooth
        }

        case "mg" => throw new RuntimeException("Merging group not supported")

        case "o" if (tokens.size >= 2) => {
          flushCurObj()

          val objName = tokens(1)
          val newObj = getObj(objName)
          curObj = Some(newObj)

          val newObjGroup = getObjGroup("default")
          curObjGroup = Some(newObjGroup)

          val newObjGroupPart = getObjGroupPart(None)
          curObjGroupPart = Some(newObjGroupPart)
        }

        // Display/render attributes

        case "bevel"    => throw new RuntimeException("Bevel not supported")

        case "c_interp" => throw new RuntimeException("Color interopolation not supported")

        case "d_interp" => throw new RuntimeException("Dissolve interpolation not supported")

        case "lod"      => throw new RuntimeException("Level of detail not supported")

        case "maplib"   => throw new RuntimeException("Library mapping not supported")

        case "usemap"   => throw new RuntimeException("Use mapping not supported")

        case "usemtl" if (tokens.size >= 2) => {
          flushCurObjGroupPart()

          val selectedMatName = tokens(1)
          val selectedMat = availableMats(selectedMatName)
          val newSubObj = getObjGroupPart(Some(selectedMat))
          curObjGroupPart = Some(newSubObj)
        }

        case "mtllib" if (tokens.size >= 2) => {
          val mtlFileContent = extraFiles(tokens(1))

          availableMats ++= parseMTL(mtlFileContent)
        }

        case "shadow_obj" => throw new RuntimeException("Shadow object not supported")

        case "trace_obj"  => throw new RuntimeException("Tracing object not supported")

        case "ctech"      => throw new RuntimeException("Curve approximation not supported")

        case "stech"      => throw new RuntimeException("Surface approximation not supported")

        // Curve and surface operation

        case "bsp"        => throw new RuntimeException("B-spline patch not supported")

        case "bzp"        => throw new RuntimeException("Bezier patch not supported")

        case "cdc"        => throw new RuntimeException("Cardinal curve not supported")

        case "cdp"        => throw new RuntimeException("Cardinal patch not supported")

        case "res"        => throw new RuntimeException("Reference and display not supported")

        // Misc

        case ""           => // Empty line (probably a comment), ignore
        case arg          => throw new RuntimeException("Unknown or invalid OBJ command \"" + arg + "\"")
      }
    }

    flushCurObj()

    objs
  }

  type Tri = (Int, Int, Int) // The three indices of the vertices of the triangle
  type VertexData = (Vector3f, Option[Vector2f], Option[Vector3f])

  case class SubTriMesh(material: Option[Material], tris: Array[Tri]) {
    override def toString(): String = material match {
      case Some(mat) => "SubTriMesh(material=\"" + mat.name + "\")"
      case None      => "SubTriMesh(no material)"
    }
  }

  case class TriMesh(name: String, vertices: Array[Vector3f], texCoordinates: Option[Array[Vector2f]],
                     normals: Option[Array[Vector3f]], submeshes: Array[SubTriMesh]) {
    override def toString(): String = "TriMesh(name=\"" + name + "\")"
  }

  def convOBJObjectToTriMesh(objs: scala.collection.Map[String, OBJObject]): scala.collection.Map[String, TriMesh] = {

    def conv(obj: OBJObject): TriMesh = {
      val subs = new ArrayBuffer[SubTriMesh]()

      val vertices = new ArrayBuffer[Vector3f]()
      val texCoordinates = new ArrayBuffer[Vector2f]()
      val normals = new ArrayBuffer[Vector3f]()

      def bufferIndexOfVertex(vertexData: VertexData): Int = {
        val (vertex, texCoordinate, normal) = vertexData

        val index = (texCoordinate, normal) match {
          case (Some(tex), Some(norm)) => {
            var i = 0
            while (i < vertices.size) {
              if (vertices(i) == vertex && texCoordinates(i) == tex && normals(i) == norm) return i
              i += 1
            }
            // No matching vertex data found, add it at the end
            vertices += vertex
            texCoordinates += tex
            normals += norm
            i
          }
          case (None, Some(norm)) => {
            var i = 0
            while (i < vertices.size) {
              if (vertices(i) == vertex && normals(i) == norm) return i
              i += 1
            }

            // No matching vertex data found, add it at the end
            vertices += vertex
            normals += norm
            i
          }
          case (Some(tex), None) => {
            var i = 0
            while (i < vertices.size) {
              if (vertices(i) == vertex && texCoordinates(i) == tex) return i
              i += 1
            }

            // No matching vertex data found, add it at the end
            vertices += vertex
            texCoordinates += tex
            i
          }
          case (None, None) => {
            var i = 0
            while (i < vertices.size) {
              if (vertices(i) == vertex) return i
              i += 1
            }

            // No matching vertex data found, add it at the end
            vertices += vertex
            i
          }
        }

        val formatErr = "The vertex data format is not uniform accross the vertices"
        if (texCoordinates.size > 0 && texCoordinates.size != vertices.size) throw new GLException(formatErr)
        if (normals.size > 0 && normals.size != vertices.size) throw new GLException(formatErr)

        index
      }

      obj.groups.foreach { group =>
        group.parts.filter { _.faces.size > 0 }.foreach { part =>
          val trisIndices = new ArrayBuffer[Tri]()

          def addTri(v0: TmpVertex, v1: TmpVertex, v2: TmpVertex): Unit = {
            def dataFromFileIndices(v: TmpVertex): VertexData = {
              val (indexV, optIndexT, optIndexN) = v

              val ova = obj.vertices(indexV - 1)
              val ov = new Vector3f(ova.x, ova.y, ova.z)
              val ot = optIndexT.map { t => val ota = obj.texCoordinates(t - 1); new Vector2f(ota.x, ota.y) }
              val on = optIndexN.map { n => obj.normals(n - 1) }

              // Data in OBJ files are indexed from 1 (instead of 0)
              (
                ov,
                ot,
                on)
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
              case 3 => {
                val v0 = face(0)
                val v1 = face(1)
                val v2 = face(2)

                addTri(v0, v1, v2)
              }
              case 4 => {
                val v0 = face(0)
                val v1 = face(1)
                val v2 = face(2)
                val v3 = face(3)

                addTri(v0, v1, v3)
                addTri(v1, v2, v3)
              }
              case _ => throw new GLException("Only faces composed of 3 of 4 vertices are supported")
            }
          }

          val newSub = new SubTriMesh(part.material, trisIndices.toArray)
          subs += newSub
        }
      }

      new TriMesh(obj.name, vertices.toArray, if (texCoordinates.size > 0) Some(texCoordinates.toArray) else None, if (normals.size > 0) Some(normals.toArray) else None, subs.toArray)
    }

    //val meshes = objs.mapValues(conv)
    val meshes = objs.map { case (name, obj) => (name, conv(obj)) }

    meshes
  }
}