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

  def getWebGLRenderingContext(gl: GLES2): dom.webgl.RenderingContext = gl match {
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
  def offsetOfElement(element: js.Any): (Int, Int) = if (element == dom.document) {
    (0, 0)
  } else {
    val dynElement = element.asInstanceOf[js.Dynamic]

    val bounding = dynElement.getBoundingClientRect()
    val window = js.Dynamic.global.window

    val boundingLeft = bounding.left.asInstanceOf[Double]
    val boundingTop = bounding.top.asInstanceOf[Double]

    val winOffsetX = window.pageXOffset.asInstanceOf[Double]
    val winOffsetY = window.pageYOffset.asInstanceOf[Double]

    val elemOffsetX = dynElement.clientLeft.asInstanceOf[Double]
    val elemOffsetY = dynElement.clientTop.asInstanceOf[Double]

    ((boundingLeft + winOffsetX - elemOffsetX).toInt, (boundingTop + winOffsetY - elemOffsetY).toInt)
  }

  object Browser {
    private val userAgent: String = js.Dynamic.global.navigator.userAgent.asInstanceOf[String].toLowerCase()

    val chrome: Boolean = userAgent.contains("chrome/")
    val firefox: Boolean = userAgent.contains("firefox/")
    val android: Boolean = userAgent.contains("android")
  }

  var orientationLockOnFullscreen: Boolean = false
  var useAuroraJs: Boolean = true
}

trait UtilsImpl extends UtilsRequirements {
  private[games] def getLoopThreadExecutionContext(): ExecutionContext = scalajs.concurrent.JSExecutionContext.Implicits.queue

  private def isHTTPCodeOk(code: Int): Boolean = (code >= 200 && code < 300) || code == 304 // HTTP Code 2xx or 304, Ok

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
      if (isHTTPCodeOk(code)) {
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
      if (isHTTPCodeOk(code)) { // HTTP Code 2xx or 304, Ok
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

    val requestAnimation = JsUtils.getOptional[js.Function1[js.Function, Unit]](g.window, "requestAnimationFrame", "webkitRequestAnimationFrame", "mozRequestAnimationFrame", "msRequestAnimationFrame", "oRequestAnimationFrame")
      .getOrElse(((fun: js.Function) => {
        g.setTimeout(fun, 1000.0 / 60.0)
        ()
      }): js.Function1[js.Function, Unit])

    def loop(): Unit = {
      if (!ctx.closed) {
        try if (fl.continue()) {
          // Main loop call
          val currentTime = JsUtils.now()
          val diff = ((currentTime - ctx.lastLoopTime) / 1e3).toFloat
          ctx.lastLoopTime = currentTime
          val frameEvent = FrameEvent(diff)
          fl.onDraw(frameEvent)
          requestAnimation(loop _)
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

    def loopInit(): Unit = {
      val readyFuture = try { fl.onCreate() } catch { case t: Throwable => Future.failed(t) }
      val ec = scalajs.concurrent.JSExecutionContext.Implicits.runNow
      readyFuture.onSuccess {
        case _ =>
          loop()
      }(ec)
      readyFuture.onFailure {
        case t => // Don't start the loop in case of failure of the given future
          Console.err.println("Could not init FrameListener")
          t.printStackTrace(Console.err)

          close()
      }(ec)

    }

    // Start listener
    requestAnimation(loopInit _)
  }
}
