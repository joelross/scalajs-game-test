package games

import java.io.InputStream
import java.io.FileInputStream
import java.io.File

object JvmResourceUtil {
  def streamForResource(res: Resource): InputStream = {
    val stream = new FileInputStream(new File("/home/joel/project-git/scalajs-games/demo/shared/src/main/resources" + res.name))
    //    val stream = JvmResourceUtil.getClass().getResourceAsStream(res.name)
    require(stream != null) // sanity check
    stream
  }
}