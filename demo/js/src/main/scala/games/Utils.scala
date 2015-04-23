package games

import scala.concurrent.{ Future, Promise, ExecutionContext }
import scala.scalajs.js
import js.Dynamic.{ global => g }
import org.scalajs.dom
import java.nio.ByteBuffer
import games.opengl.GLES2
import games.opengl.GLES2WebGL
import games.opengl.GLES2Debug
import scala.collection.mutable.Queue

object JsUtils {
  var autoToggling = false

  private val userEventTasks: Queue[Runnable] = Queue()

  def flushUserEventTasks(): Unit = {
    userEventTasks.foreach { runnable =>
      try {
        runnable.run()
      } catch {
        case t: Throwable => userEventExecutionContext.reportFailure(t)
      }
    }

    userEventTasks.clear()
  }

  def addUserEventTask(runnable: Runnable): Unit = {
    userEventTasks += runnable
  }

  val userEventExecutionContext: ExecutionContext = new ExecutionContext() {
    def execute(runnable: Runnable): Unit = addUserEventTask(runnable)
    def reportFailure(cause: Throwable): Unit = ExecutionContext.defaultReporter(cause)
  }

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

  def getOptional[T](el: js.Dynamic, fields: String*): Option[T] = {
    def getOptionalJS(fields: String*): js.UndefOr[T] = {
      if (fields.isEmpty) js.undefined
      else el.selectDynamic(fields.head).asInstanceOf[js.UndefOr[T]].orElse(getOptionalJS(fields.tail: _*))
    }

    getOptionalJS(fields: _*).toOption
  }

  def featureUnsupportedText(feature: String): String = {
    "Feature " + feature + " not supported"
  }

  def featureUnsupportedFunction(feature: String): js.Function = {
    () => { Console.err.println(featureUnsupportedText(feature)) }
  }

  def throwFeatureUnsupported(feature: String): Nothing = {
    throw new RuntimeException(featureUnsupportedText(feature))
  }

  private val typeRegex = js.Dynamic.newInstance(g.RegExp)("^\\[object\\s(.*)\\]$")

  /*
   * Return the type of the JavaScript object as a String. Examples:
   * 1.5 -> Number
   * true -> Boolean
   * "Hello" -> String
   * null -> Null
   */
  def typeName(jsObj: js.Any): String = {
    val fullName = g.Object.prototype.selectDynamic("toString").call(jsObj).asInstanceOf[String]
    val execArray = typeRegex.exec(fullName).asInstanceOf[js.Array[String]]
    val name = execArray(1)
    name
  }

  /*
   * Get the offset of the element.
   * From jQuery: https://github.com/jquery/jquery/blob/2.1.3/src/offset.js#L107-L108
   */
  def offsetOfElement(element: js.Dynamic): (Int, Int) = {
    val bounding = element.getBoundingClientRect()
    val window = js.Dynamic.global.window

    val boundingLeft = bounding.left.asInstanceOf[Double]
    val boundingTop = bounding.top.asInstanceOf[Double]

    val winOffsetX = window.pageXOffset.asInstanceOf[Double]
    val winOffsetY = window.pageYOffset.asInstanceOf[Double]

    val elemOffsetX = element.clientLeft.asInstanceOf[Double]
    val elemOffsetY = element.clientTop.asInstanceOf[Double]

    ((boundingLeft + winOffsetX - elemOffsetX).toInt, (boundingTop + winOffsetY - elemOffsetY).toInt)
  }
}

trait UtilsImpl extends UtilsRequirements {
  def getLoopThreadExecutionContext(): ExecutionContext = scalajs.concurrent.JSExecutionContext.Implicits.queue

  def getBinaryDataFromResource(res: games.Resource)(implicit ec: ExecutionContext): scala.concurrent.Future[java.nio.ByteBuffer] = {
    val xmlRequest = new dom.XMLHttpRequest()

    val path = JsUtils.pathForResource(res)

    xmlRequest.open("GET", path, true)
    xmlRequest.responseType = "arraybuffer"
    xmlRequest.asInstanceOf[js.Dynamic].overrideMimeType("application/octet-stream")

    val promise = Promise[ByteBuffer]

    def error(): String = "Could not binary text resource " + res + ": code " + xmlRequest.status + " (" + xmlRequest.statusText + ")"

    xmlRequest.onload = (e: dom.Event) => {
      val code = xmlRequest.status
      if (code >= 200 && code < 300 || code == 304) { // HTTP Code 2xx or 304, Ok
        val arrayBuffer = xmlRequest.response.asInstanceOf[js.typedarray.ArrayBuffer]
        val byteBuffer = js.typedarray.TypedArrayBuffer.wrap(arrayBuffer)
        promise.success(byteBuffer)
      } else {
        promise.failure(new RuntimeException(error()))
      }
    }
    xmlRequest.onerror = (e: dom.Event) => {
      promise.failure(new RuntimeException(error()))
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

    def error(): String = "Could not retrieve text resource " + res + ": code " + xmlRequest.status + " (" + xmlRequest.statusText + ")"

    xmlRequest.onload = (e: dom.Event) => {
      val code = xmlRequest.status
      if (code >= 200 && code < 300 || code == 304) { // HTTP Code 2xx or 304, Ok
        val text: String = xmlRequest.responseText
        promise.success(text)
      } else {
        promise.failure(new RuntimeException(error()))
      }
    }
    xmlRequest.onerror = (e: dom.Event) => {
      promise.failure(new RuntimeException(error()))
    }

    xmlRequest.send(null)

    promise.future
  }
  def loadTexture2DFromResource(res: games.Resource, texture: games.opengl.Token.Texture, gl: games.opengl.GLES2, openglExecutionContext: ExecutionContext, preload: => Boolean = true)(implicit ec: ExecutionContext): scala.concurrent.Future[Unit] = {
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

    image.src = JsUtils.pathForResource(res)

    promise.future
  }
  def startFrameListener(fl: games.FrameListener): Unit = {
    class FrameListenerLoopContext {
      var lastLoopTime: Double = JsUtils.now()
      var closed: Boolean = false
    }

    val ctx = new FrameListenerLoopContext

    def close(): Unit = {
      ctx.closed = true
      fl.onClose()
    }

    def loop(timeStamp: js.Any): Unit = {
      if (!ctx.closed) {
        try if (fl.continue()) {
          // Main loop call
          val currentTime = JsUtils.now()
          val diff = ((currentTime - ctx.lastLoopTime) / 1e3).toFloat
          ctx.lastLoopTime = currentTime
          val frameEvent = FrameEvent(diff)
          fl.onDraw(frameEvent)
          g.window.requestAnimationFrame(loop _)
        } else {
          close()
        } catch {
          case t: Throwable =>
            Console.err.println("Error during looping of FrameListener")
            t.printStackTrace(Console.err)

            close()
        }
      }
    }

    def loopInit(timeStamp: js.Any): Unit = {
      val readyOptFuture = try { fl.onCreate() } catch { case t: Throwable => Some(Future.failed(t)) }
      readyOptFuture match {
        case None => loop(timeStamp) // ready right now

        case Some(future) => // wait for the future to complete
          val ec = scalajs.concurrent.JSExecutionContext.Implicits.runNow
          future.onSuccess {
            case _ =>
              loop(timeStamp)
          }(ec)
          future.onFailure {
            case t => // Don't start the loop in case of failure of the given future
              Console.err.println("Could not init FrameListener")
              t.printStackTrace(Console.err)

              close()
          }(ec)

      }

    }

    // Start listener
    g.requestAnimationFrame(loopInit _)
  }
}