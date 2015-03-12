package games

import scala.concurrent.{ Future, Promise, ExecutionContext }
import scala.scalajs.js
import js.Dynamic.{ global => g }
import org.scalajs.dom
import java.nio.ByteBuffer
import games.opengl.GLES2
import games.opengl.GLES2WebGL
import games.opengl.GLES2Debug

object JsUtils {
  private var relativeResourcePath: Option[String] = None

  def pathForResource(res: Resource): String = relativeResourcePath match {
    case Some(path) => path + res.name
    case None       => throw new RuntimeException("Relative path must be defined before calling pathForResource")
  }

  def setResourcePath(path: String): Unit = {
    relativeResourcePath = Some(path)
  }

  // TODO performance.now() for microseconds precision: https://developer.mozilla.org/en-US/docs/Web/API/Performance.now%28%29
  def now(): Double = g.Date.now().asInstanceOf[Double]

  def getWebGLRenderingContext(gl: GLES2): dom.raw.WebGLRenderingContext = gl match {
    case gles2webgl: GLES2WebGL => gles2webgl.getWebGLRenderingContext()
    case gles2debug: GLES2Debug => getWebGLRenderingContext(gles2debug.getInternalContext())
    case _                      => throw new RuntimeException("Could not retrieve the WebGLRenderingContext from GLES2")
  }
}

trait UtilsImpl extends UtilsRequirements {
  def getBinaryDataFromResource(res: games.Resource)(implicit ec: ExecutionContext): scala.concurrent.Future[java.nio.ByteBuffer] = {
    val xmlRequest = new dom.XMLHttpRequest()

    val path = JsUtils.pathForResource(res)

    xmlRequest.open("GET", path, true)
    xmlRequest.responseType = "arraybuffer"
    xmlRequest.asInstanceOf[js.Dynamic].overrideMimeType("application/octet-stream")

    val promise = Promise[ByteBuffer]

    xmlRequest.onload = (e: dom.Event) => {
      val arrayBuffer = xmlRequest.response.asInstanceOf[js.typedarray.ArrayBuffer]
      val byteBuffer = js.typedarray.TypedArrayBuffer.wrap(arrayBuffer)
      promise.success(byteBuffer)
    }
    xmlRequest.onerror = (e: dom.Event) => {
      promise.failure(new RuntimeException("Could not retrieve binary resource " + res + ": " + xmlRequest.statusText))
    }

    xmlRequest.send(null)

    promise.future
  }
  def getTextDataFromResource(res: games.Resource)(implicit ec: ExecutionContext): scala.concurrent.Future[String] = {
    val xmlRequest = new dom.XMLHttpRequest()

    val path = JsUtils.pathForResource(res)

    xmlRequest.open("GET", path, true)
    xmlRequest.responseType = "text"
    xmlRequest.asInstanceOf[js.Dynamic].overrideMimeType("text/plain")

    val promise = Promise[String]

    xmlRequest.onload = (e: dom.Event) => {
      val text: String = xmlRequest.responseText
      promise.success(text)
    }
    xmlRequest.onerror = (e: dom.Event) => {
      promise.failure(new RuntimeException("Could not retrieve text resource " + res + ": " + xmlRequest.statusText))
    }

    xmlRequest.send(null)

    promise.future
  }
  def loadTexture2DFromResource(res: games.Resource, texture: games.opengl.Token.Texture, preload: => Boolean = true)(implicit gl: games.opengl.GLES2, ec: ExecutionContext): scala.concurrent.Future[Unit] = {
    val image = dom.document.createElement("img").asInstanceOf[js.Dynamic]

    val promise = Promise[Unit]

    image.onload = () => {
      try {
        if (preload) throw new RuntimeException("Texture loading cancelled by user")

        val previousTexture = gl.getParameterTexture(GLES2.TEXTURE_BINDING_2D)
        gl.bindTexture(GLES2.TEXTURE_2D, texture)

        val webglRenderingContext = JsUtils.getWebGLRenderingContext(gl)
        val webglCtx = webglRenderingContext.asInstanceOf[js.Dynamic]
        webglCtx.pixelStorei(webglCtx.UNPACK_FLIP_Y_WEBGL, false)
        webglRenderingContext.texImage2D(GLES2.TEXTURE_2D, 0, GLES2.RGBA, GLES2.RGBA, GLES2.UNSIGNED_BYTE, image.asInstanceOf[dom.html.Image])
        gl.bindTexture(GLES2.TEXTURE_2D, previousTexture)

        promise.success((): Unit)
      } catch {
        case t: Throwable => promise.failure(t)
      }
    }
    image.onerror = () => {
      promise.failure(new RuntimeException("Could not retrieve image " + res))
    }

    promise.future
  }
  def startFrameListener(fl: games.FrameListener): Unit = {
    class FrameListenerLoopContext {
      var lastLoopTime: Double = JsUtils.now()
      var closed: Boolean = false
      var dim: (Int, Int) = _
    }

    val ctx = new FrameListenerLoopContext

    val renderingContext = JsUtils.getWebGLRenderingContext(fl.context)
    val canvas = renderingContext.canvas

    def loop(timeStamp: js.Any): Unit = {
      if (!ctx.closed) {
        if (fl.continue()) {
          // Check whether the canvas dimension has changed
          val newDim = (canvas.width, canvas.height)
          if (newDim != ctx.dim) {
            ctx.dim = newDim
            val (width, height) = newDim
            fl.onChange(width, height)
          }

          // Main loop call
          val currentTime = JsUtils.now()
          val diff = ((currentTime - ctx.lastLoopTime) / 1e3).toFloat
          ctx.lastLoopTime = currentTime
          val frameEvent = FrameEvent(diff)
          fl.onDraw(frameEvent)
          g.window.requestAnimationFrame(loop _)
        } else {
          ctx.closed = true
          fl.onClose()
        }
      }
    }

    def loopInit(timeStamp: js.Any): Unit = {
      fl.onCreate()
      val width = canvas.width
      val height = canvas.height
      ctx.dim = (width, height)
      fl.onChange(width, height)
      loop(timeStamp)
    }

    // Start listener
    g.requestAnimationFrame(loopInit _)
  }
}