package games.audio

import scala.scalajs.js
import org.scalajs.dom
import games.Resource
import games.math.Vector3f
import games.JsUtils
import games.Utils

import java.nio.{ ByteBuffer, ByteOrder }

import scala.collection.mutable.Set
import scala.concurrent.{ Promise, Future }
import scalajs.concurrent.JSExecutionContext.Implicits.queue

import scala.collection.{ mutable, immutable }

import js.Dynamic.{ global => g }

private[games] object AuroraHelper {
  def createDataFromAurora(ctx: WebAudioContext, arraybuffer: js.typedarray.ArrayBuffer): scala.concurrent.Future[JsBufferData] = {
    val promise = Promise[JsBufferData]

    val asset = js.Dynamic.global.AV.Asset.fromBuffer(arraybuffer)
    asset.on("error", (error: String) => {
      promise.failure(new RuntimeException("Aurora returned error: " + error))
    })

    asset.decodeToBuffer((data: js.typedarray.Float32Array) => {
      val arraybuffer = data.buffer
      val byteBuffer = js.typedarray.TypedArrayBuffer.wrap(arraybuffer)

      var optFormat: Option[js.Dynamic] = None
      asset.get("format", (format: js.Dynamic) => {
        optFormat = Some(format)
      })

      optFormat match {
        case Some(format) =>
          val channels = format.channelsPerFrame.asInstanceOf[Int]
          val sampleRate = format.sampleRate.asInstanceOf[Int]
          val dataFuture = ctx.prepareRawData(byteBuffer, Format.Float32, channels, sampleRate)
          dataFuture.onSuccess { case data => promise.success(data) }
          dataFuture.onFailure { case t => promise.failure(new RuntimeException("Aurora decoded successfully, but could not create the Web Audio buffer", t)) }

        case None =>
          promise.failure(new RuntimeException("Decoding done, but failed to retrieve the format from Aurora"))
      }
    })

    promise.future
  }

  def createDataFromAurora(ctx: WebAudioContext, res: Resource): scala.concurrent.Future[JsBufferData] = {
    Utils.getBinaryDataFromResource(res).flatMap { bb =>
      import scala.scalajs.js.typedarray.TypedArrayBufferOps._

      val arrayBuffer = bb.arrayBuffer()
      this.createDataFromAurora(ctx, arrayBuffer)
    }
  }
}

object WebAudioContext {
  lazy val auroraPresent: Boolean = {
    JsUtils.getOptional[js.Dynamic](js.Dynamic.global, "AV").flatMap { av => JsUtils.getOptional[js.Dynamic](av, "Asset") }.isDefined
  }

  def canUseAurora: Boolean = JsUtils.useAuroraJs && auroraPresent
}

class WebAudioContext extends Context {
  private val audioContext: js.Dynamic = JsUtils.getOptional[js.Dynamic](g, "AudioContext", "webkitAudioContext").getOrElse(throw new RuntimeException("Web Audio API not supported by your browser"))
  private[games] val webApi = js.Dynamic.newInstance(audioContext)()

  private[games] val mainOutput = {
    val node = webApi.createGain()
    node.connect(webApi.destination)
    node.gain.value = 1.0
    node
  }

  def prepareStreamingData(res: Resource): Future[games.audio.Data] = {
    // Streaming data is not a good idea on Android Chrome: https://code.google.com/p/chromium/issues/detail?id=138132#c6
    if (JsUtils.Browser.chrome && JsUtils.Browser.android) {
      Console.err.println("Warning: Android Chrome does not support streaming data (resource " + res + "), switching to buffered data")
      this.prepareBufferedData(res)
    } else Future.successful(new JsStreamingData(this, res))
  }
  def prepareBufferedData(res: Resource): Future[games.audio.JsBufferData] = {
    val dataFuture = Utils.getBinaryDataFromResource(res)
    val promise = Promise[JsBufferData]

    dataFuture.map { bb =>
      import scala.scalajs.js.typedarray.TypedArrayBufferOps._

      val arraybuffer = bb.arrayBuffer()
      this.webApi.decodeAudioData(arraybuffer,
        (decodedBuffer: js.Dynamic) => {
          promise.success(new JsBufferData(this, decodedBuffer))
        },
        () => {
          val msg = "Failed to decode the audio data from resource " + res
          // If Aurora is available and this error seems due to decoding, try with Aurora
          if (WebAudioContext.canUseAurora) {
            val auroraDataFuture = AuroraHelper.createDataFromAurora(this, arraybuffer)
            auroraDataFuture.onSuccess { case auroraData => promise.success(auroraData) }
            auroraDataFuture.onFailure { case t => promise.failure(new RuntimeException(msg + " (result with Aurora: " + t + ")", t)) }
          } else {
            promise.failure(new RuntimeException(msg))
          }
        })
    }

    promise.future
  }
  def prepareRawData(data: ByteBuffer, format: Format, channels: Int, freq: Int): Future[games.audio.JsBufferData] = Future {
    format match {
      case Format.Float32 => // good to go
      case _              => throw new RuntimeException("Unsupported data format: " + format)
    }

    channels match {
      case 1 => // good to go
      case 2 => // good to go
      case _ => throw new RuntimeException("Unsupported channels number: " + channels)
    }

    val floatBuffer = data.slice().order(ByteOrder.nativeOrder()).asFloatBuffer()

    val sampleCount = floatBuffer.remaining() / channels

    val buffer = this.webApi.createBuffer(channels, sampleCount, freq)

    var channelsData = new Array[js.typedarray.Float32Array](channels)

    for (channelCur <- 0 until channels) {
      channelsData(channelCur) = buffer.getChannelData(channelCur).asInstanceOf[js.typedarray.Float32Array]
    }

    for (sampleCur <- 0 until sampleCount) {
      for (channelCur <- 0 until channels) {
        channelsData(channelCur)(sampleCur) = floatBuffer.get()
      }
    }

    new JsBufferData(this, buffer)
  }

  def createSource(): JsSource = new JsSource(this, mainOutput)
  def createSource3D(): JsSource3D = new JsSource3D(this, mainOutput)

  val listener: JsListener = new JsListener(this)

  def volume: Float = mainOutput.gain.value.asInstanceOf[Double].toFloat
  def volume_=(volume: Float) = mainOutput.gain.value = volume.toDouble

  override def close(): Unit = {
    super.close()
  }
}

class JsListener private[games] (ctx: WebAudioContext) extends Listener {
  private val orientationData = new Vector3f(0, 0, -1)
  private val upData = new Vector3f(0, 1, 0)
  private val positionData = new Vector3f(0, 0, 0)

  // Init
  ctx.webApi.listener.setPosition(positionData.x.toDouble, positionData.y.toDouble, positionData.z.toDouble)
  ctx.webApi.listener.setOrientation(orientationData.x.toDouble, orientationData.y.toDouble, orientationData.z.toDouble, upData.x.toDouble, upData.y.toDouble, upData.z.toDouble)

  def orientation: Vector3f = orientationData.copy()
  def position: Vector3f = positionData.copy()
  def position_=(position: Vector3f): Unit = {
    Vector3f.set(position, positionData)
    ctx.webApi.listener.setPosition(positionData.x.toDouble, positionData.y.toDouble, positionData.z.toDouble)
  }
  def up: Vector3f = upData.copy()
  def setOrientation(orientation: Vector3f, up: Vector3f): Unit = {
    Vector3f.set(orientation, orientationData)
    Vector3f.set(up, upData)
    ctx.webApi.listener.setOrientation(orientationData.x.toDouble, orientationData.y.toDouble, orientationData.z.toDouble, upData.x.toDouble, upData.y.toDouble, upData.z.toDouble)
  }
}