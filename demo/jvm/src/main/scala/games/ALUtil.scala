package games

import java.nio.ByteOrder
import java.nio.ByteBuffer

object ALUtil {
  val tmpIntBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()
}