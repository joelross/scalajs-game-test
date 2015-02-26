package games

object JsResourceUtil {
  private var relativeResourcePath: Option[String] = None

  def pathForResource(res: Resource): String = relativeResourcePath match {
    case Some(path) => path + res.name
    case None       => throw new RuntimeException("Relative path must be defined before calling pathForResource")
  }

  def setRelativePath(path: String): Unit = {
    relativeResourcePath = Some(path)
  }
}