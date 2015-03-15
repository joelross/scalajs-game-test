package games

import java.util.concurrent.ConcurrentLinkedQueue
import java.io.InputStream
import scala.concurrent.{ Future, ExecutionContext }
import java.io.ByteArrayOutputStream
import java.nio.{ ByteBuffer, ByteOrder }
import org.lwjgl.opengl._
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.imageio.ImageIO

import games.opengl.GLES2

object JvmUtils {
  private val pendingTaskList = new ConcurrentLinkedQueue[Runnable]

  /**
   * Trivial ExecutionContext that simply execute the Runnable immediately in the same thread
   */
  val immediateExecutionContext: ExecutionContext = new ExecutionContext {
    def execute(runnable: Runnable): Unit = {
      try {
        runnable.run()
      } catch {
        case t: Throwable => this.reportFailure(t)
      }
    }
    def reportFailure(cause: Throwable): Unit = ExecutionContext.defaultReporter(cause)
  }

  implicit val openglExecutionContext: ExecutionContext = new ExecutionContext {
    def execute(runnable: Runnable): Unit = addPendingTask(runnable)
    def reportFailure(cause: Throwable): Unit = ExecutionContext.defaultReporter(cause)
  }

  /**
   * Flush all the pending tasks.
   * You don't need to explicitly use this method if you use the FrameListener loop system.
   * Warning: this should be called only from the OpenGL thread!
   */
  def flushPendingTaskList(): Unit = {
    var current: Runnable = null
    while ({ current = pendingTaskList.poll(); current } != null) {
      try {
        current.run()
      } catch {
        case t: Throwable => openglExecutionContext.reportFailure(t)
      }
    }
  }

  /**
   * Add a Runnable task to be executed by the OpenGL thread.
   * Tasks are usually executed at the beginning of the next iteration of the FrameListener loop system.
   */
  def addPendingTask(task: Runnable): Unit = {
    pendingTaskList.add(task)
  }

  def streamForResource(res: Resource): InputStream = {
    val stream = JvmUtils.getClass().getResourceAsStream(res.name)
    if (stream == null) throw new RuntimeException("Could not load resource " + res.name)
    stream
  }
}

trait UtilsImpl extends UtilsRequirements {
  def getBinaryDataFromResource(res: games.Resource)(implicit ec: ExecutionContext): scala.concurrent.Future[java.nio.ByteBuffer] = {
    Future {
      val stream = JvmUtils.streamForResource(res)
      val byteStream = new ByteArrayOutputStream()
      val tmpData: Array[Byte] = new Array[Byte](4096) // 4KiB of temp data

      var tmpDataContentSize: Int = 0
      while ({ tmpDataContentSize = stream.read(tmpData); tmpDataContentSize } >= 0) {
        byteStream.write(tmpData, 0, tmpDataContentSize)
      }

      stream.close()

      val byteArray = byteStream.toByteArray()
      val byteBuffer = ByteBuffer.allocate(byteArray.length)

      byteBuffer.put(byteArray)
      byteBuffer.rewind()

      byteBuffer
    }
  }
  def getTextDataFromResource(res: games.Resource)(implicit ec: ExecutionContext): scala.concurrent.Future[String] = {
    Future {
      val stream = JvmUtils.streamForResource(res)
      val streamReader = new InputStreamReader(stream)
      val reader = new BufferedReader(streamReader)

      val text = new StringBuilder()

      val buffer = Array[Char](4096) // 4KiB buffer
      var bufferReadLength = 0

      while ({ bufferReadLength = reader.read(buffer); bufferReadLength } >= 0) {
        text.appendAll(buffer, 0, bufferReadLength)
      }

      reader.close()
      streamReader.close()
      stream.close()

      text.toString()
    }
  }
  def loadTexture2DFromResource(res: games.Resource, texture: games.opengl.Token.Texture, preload: => Boolean = true)(implicit gl: games.opengl.GLES2, ec: ExecutionContext): scala.concurrent.Future[Unit] = {
    Future {
      val stream = JvmUtils.streamForResource(res)

      // Should support JPEG, PNG, BMP, WBMP and GIF
      val image = ImageIO.read(stream)

      val height = image.getHeight()
      val width = image.getWidth()
      val byteBuffer = GLES2.createByteBuffer(4 * width * height) // Stored as RGBA value: 4 bytes per pixel
      val tmp = new Array[Byte](4)
      var y = 0
      while (y < height) {
        var x = 0
        while (x < width) {
          val argb = image.getRGB(x, y)
          tmp(2) = argb.toByte // blue
          tmp(1) = (argb >> 8).toByte // green
          tmp(0) = (argb >> 16).toByte // red
          tmp(3) = (argb >> 24).toByte // alpha
          byteBuffer.put(tmp)
          x += 1
        }
        y += 1
      }
      stream.close()
      byteBuffer.rewind

      (width, height, byteBuffer)
    }.map {
      case (width, height, byteBuffer) =>
        if (!preload) throw new RuntimeException("Texture loading cancelled by user")

        val previousTexture = gl.getParameterTexture(GLES2.TEXTURE_BINDING_2D)
        gl.bindTexture(GLES2.TEXTURE_2D, texture)
        gl.texImage2D(GLES2.TEXTURE_2D, 0, GLES2.RGBA, width, height, 0, GLES2.RGBA, GLES2.UNSIGNED_BYTE, byteBuffer)
        gl.bindTexture(GLES2.TEXTURE_2D, previousTexture)
    }(JvmUtils.openglExecutionContext)
  }
  def startFrameListener(fl: games.FrameListener): Unit = {
    def screenDim(): (Int, Int) = {
      val displayMode = Display.getDisplayMode()

      val width = displayMode.getWidth()
      val height = displayMode.getHeight()

      (width, height)
    }

    val frameListenerThread = new Thread(new Runnable {
      def run() {
        var lastLoopTime: Long = System.nanoTime()
        fl.onCreate()

        while (fl.continue()) {
          // Execute the pending tasks
          JvmUtils.flushPendingTaskList()

          // Main loop call
          val currentTime: Long = System.nanoTime()
          val diff = ((currentTime - lastLoopTime) / 1e9).toFloat
          lastLoopTime = currentTime
          val frameEvent = FrameEvent(diff)
          fl.onDraw(frameEvent)
        }
        fl.onClose()
      }
    })
    // Start listener
    frameListenerThread.start()
  }
}