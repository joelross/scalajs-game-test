package games

import java.util.concurrent.ConcurrentLinkedQueue
import java.io.InputStream
import scala.concurrent.{ Future, Promise, ExecutionContext }
import java.io.ByteArrayOutputStream
import java.nio.{ ByteBuffer, ByteOrder }
import org.lwjgl.opengl._
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.imageio.ImageIO

import games.opengl.GLES2

class FixedExecutionContextFuture[T](realFuture: Future[T], ec: ExecutionContext) extends Future[T] {
  // Members declared in scala.concurrent.Awaitable
  def ready(atMost: scala.concurrent.duration.Duration)(implicit permit: scala.concurrent.CanAwait): this.type = { realFuture.ready(atMost)(permit); this }
  def result(atMost: scala.concurrent.duration.Duration)(implicit permit: scala.concurrent.CanAwait): T = realFuture.result(atMost)(permit)

  // Members declared in scala.concurrent.Future
  def isCompleted: Boolean = realFuture.isCompleted
  def onComplete[U](f: scala.util.Try[T] => U)(implicit executor: scala.concurrent.ExecutionContext): Unit = realFuture.onComplete(f)(ec)
  def value: Option[scala.util.Try[T]] = realFuture.value
}

object JvmUtils {
  private val pendingTaskList = new ConcurrentLinkedQueue[() => Unit]

  def wrapFuture[T](future: Future[T]): Future[T] = new FixedExecutionContextFuture(future, JvmUtils.immediateExecutionContext)

  def doAsync[T](fun: => T)(implicit ec: ExecutionContext): Future[T] = {
    val promise = Promise[T]

    val prepare: Future[T] = Future { fun }(ec)
    // The immediateExecutionContext is not necessary here, but as we are simply redirecting to other promises, the additional threading would provide no gain
    prepare.onSuccess { case retValue => JvmUtils.addPendingTask { () => promise.success(retValue) } }(JvmUtils.immediateExecutionContext)
    prepare.onFailure { case t: Throwable => JvmUtils.addPendingTask { () => promise.failure(t) } }(JvmUtils.immediateExecutionContext)

    wrapFuture(promise.future)
  }

  /**
   * Trivial ExecutionContext that simply execute the Runnable immediately in the same thread (useful to make sure it happens
   * on the OpenGL thread)
   */
  implicit val immediateExecutionContext: ExecutionContext = new ExecutionContext {
    def execute(runnable: Runnable): Unit = {
      try {
        runnable.run()
      } catch {
        case t: Throwable => this.reportFailure(t)
      }
    }
    def reportFailure(cause: Throwable): Unit = ExecutionContext.defaultReporter(cause)
  }

  /**
   * Flush all the pending tasks.
   * You don't need to explicitly use this method if you use the FrameListener loop system.
   * Warning: this should be called only from the OpenGL thread!
   */
  def flushPendingTaskList(): Unit = {
    var current: () => Unit = null
    while ({ current = pendingTaskList.poll(); current } != null) {
      current()
    }
  }
  /**
   * Add a task to be executed by the OpenGL thread.
   * Tasks are usually executed at the beginning of the next iteration of the FrameListener loop system.
   */
  def addPendingTask(task: () => Unit): Unit = {
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
    JvmUtils.doAsync {
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
    JvmUtils.doAsync {
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
    val promise = Promise[Unit]

    Future {
      try {
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

        // Don't load it now, we want it done synchronously in the main loop to avoid concurrency issue
        JvmUtils.addPendingTask { () =>
          try {
            if (!preload) throw new RuntimeException("Texture loading cancelled by user")

            val previousTexture = gl.getParameterTexture(GLES2.TEXTURE_BINDING_2D)
            gl.bindTexture(GLES2.TEXTURE_2D, texture)
            gl.texImage2D(GLES2.TEXTURE_2D, 0, GLES2.RGBA, width, height, 0, GLES2.RGBA, GLES2.UNSIGNED_BYTE, byteBuffer)
            gl.bindTexture(GLES2.TEXTURE_2D, previousTexture)

            promise.success((): Unit)
          } catch {
            case t: Throwable => promise.failure(t)
          }
        }
      } catch {
        case t: Throwable => JvmUtils.addPendingTask { () => promise.failure(t) }
      }
    }

    JvmUtils.wrapFuture(promise.future)
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