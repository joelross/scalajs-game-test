package games

import java.io.InputStream
import java.io.FileInputStream
import java.io.File

object JvmResourceUtil {
  def streamForResource(res: Resource): InputStream = {
    val stream = JvmResourceUtil.getClass().getResourceAsStream(res.name)
    if (stream == null) throw new RuntimeException("Could not load resource " + res.name) // TODO remove later, sanity check
    stream
  }
}