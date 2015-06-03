package games

import java.util.concurrent.ConcurrentLinkedQueue
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.nio.{ ByteBuffer, ByteOrder }
import org.lwjgl.opengl._
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.imageio.ImageIO

import scala.concurrent.{ Await, Future, ExecutionContext }
import scala.concurrent.duration.Duration
import scala.util.{ Success, Failure }

import games.opengl.GLES2

private[games] class ExplicitExecutionContext extends ExecutionContext {
  private val pendingRunnables = new ConcurrentLinkedQueue[Runnable]

  def execute(runnable: Runnable): Unit = {
    pendingRunnables.add(runnable)
  }
  def reportFailure(cause: Throwable): Unit = {
    ExecutionContext.defaultReporter(cause)
  }

  /**
   * Flush all the currently pending runnables.
   * You don't need to explicitly use this method if you use the FrameListener loop system.
   * Warning: this should be called only from the OpenGL thread!
   */
  def flushPending(): Unit = {
    var current: Runnable = null
    while ({ current = pendingRunnables.poll(); current } != null) {
      try { current.run() }
      catch { case t: Throwable => this.reportFailure(t) }
    }
  }
}

object JvmUtils {
  def streamForResource(res: Resource): InputStream = {
    val stream = JvmUtils.getClass().getResourceAsStream(res.name)
    if (stream == null) throw new RuntimeException("Could not retrieve resource " + res.name)
    stream
  }
}

trait UtilsImpl extends UtilsRequirements {
  def getLoopThreadExecutionContext(): ExecutionContext = new ExplicitExecutionContext

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
  def loadTexture2DFromResource(res: games.Resource, texture: games.opengl.Token.Texture, gl: games.opengl.GLES2, openglExecutionContext: ExecutionContext, preload: => Boolean = true)(implicit ec: ExecutionContext): scala.concurrent.Future[Unit] = {
    Future {
      val stream = JvmUtils.streamForResource(res)

      // Should support JPEG, PNG, BMP, WBMP and GIF
      val image = ImageIO.read(stream)

      val height = image.getHeight()
      val width = image.getWidth()
      val byteBuffer = GLES2.createByteBuffer(4 * width * height) // Stored as RGBA value: 4 bytes per pixel
      val intBuffer = byteBuffer.duplicate().order(ByteOrder.BIG_ENDIAN).asIntBuffer()
      val tmp = new Array[Byte](4)
      for (y <- 0 until height) {
        for (x <- 0 until width) {
          val argb = image.getRGB(x, y)
          intBuffer.put((argb >>> 24) | (argb << 8))
        }
      }
      stream.close()

      (width, height, byteBuffer)
    }.map { // Execute this part with the openglExecutionContext instead of the standard one
      case (width, height, byteBuffer) =>
        if (!preload) throw new RuntimeException("Texture loading cancelled by user")

        val previousTexture = gl.getParameterTexture(GLES2.TEXTURE_BINDING_2D)
        gl.bindTexture(GLES2.TEXTURE_2D, texture)
        gl.texImage2D(GLES2.TEXTURE_2D, 0, GLES2.RGBA, width, height, 0, GLES2.RGBA, GLES2.UNSIGNED_BYTE, byteBuffer)
        gl.bindTexture(GLES2.TEXTURE_2D, previousTexture)
    }(openglExecutionContext)
  }
  def startFrameListener(fl: games.FrameListener): Unit = {
    val frameListenerThread = new Thread(new Runnable {
      def run() {
        var lastLoopTime: Long = System.nanoTime()
        val readyOptFuture = try { fl.onCreate() } catch { case t: Throwable => Some(Future.failed(t)) }

        var looping = true

        readyOptFuture match {
          case None => // just continue
          case Some(future) => // wait for it
            while (!future.isCompleted) {
              // Execute the pending tasks
              fl.loopExecutionContext.asInstanceOf[ExplicitExecutionContext].flushPending()
              Thread.sleep(100) // Don't exhaust the CPU, 10Hz should be enough
            }

            future.value.get match {
              case Success(_) => // Ok, nothing to do, just continue
              case Failure(t) =>
                Console.err.println("Could not init FrameListener")
                t.printStackTrace(Console.err)
                looping = false
            }
        }

        try while (looping && fl.continue()) {
          // Execute the pending tasks
          fl.loopExecutionContext.asInstanceOf[ExplicitExecutionContext].flushPending()

          // Main loop call
          val currentTime: Long = System.nanoTime()
          val diff = ((currentTime - lastLoopTime) / 1e9).toFloat
          lastLoopTime = currentTime
          val frameEvent = FrameEvent(diff)
          fl.onDraw(frameEvent)

          Display.update()
        } catch {
          case t: Throwable =>
            Console.err.println("Error during looping of FrameListener")
            t.printStackTrace(Console.err)
        }

        fl.onClose()
      }
    })
    // Start listener
    frameListenerThread.start()
  }
}