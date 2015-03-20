package games.audio

import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10
import org.lwjgl.openal.Util
import games.math.Vector3f

import java.nio.ByteBuffer
import java.nio.ByteOrder

import scala.collection.mutable.Set

class ALContext extends Context {
  AL.create()

  def createBufferedData(res: games.Resource): games.audio.BufferedData = new ALBufferedData(this, res)
  def createRawData(data: ByteBuffer, format: Format, channels: Int, freq: Int): games.audio.RawData = new ALRawData(this, data, format, channels, freq)
  def createStreamingData(res: games.Resource): games.audio.StreamingData = new ALStreamingData(this, res)

  override def close(): Unit = {
    AL.destroy()
  }

  val listener: Listener = new ALListener()

  private[games] var masterVolume = 1f

  def volume: Float = masterVolume
  def volume_=(volume: Float) = {
    masterVolume = volume
    sources.foreach { source => source.masterVolumeChanged() }
  }

  private val sources: Set[ALSource] = Set()
  private[games] def addSource(source: ALSource): Unit = {
    sources += source
  }
  private[games] def removeSource(source: ALSource): Unit = {
    sources -= source
  }
}

class ALListener private[games] () extends Listener {
  private val orientationBuffer = ByteBuffer.allocateDirect(2 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
  private val positionBuffer = ByteBuffer.allocateDirect(1 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

  // Preload buffer
  AL10.alGetListener(AL10.AL_POSITION, positionBuffer)
  AL10.alGetListener(AL10.AL_ORIENTATION, orientationBuffer)
  Util.checkALError()

  def position: Vector3f = {
    positionBuffer.rewind()
    val ret = new Vector3f
    ret.load(positionBuffer)
    ret
  }
  def position_=(position: Vector3f): Unit = {
    positionBuffer.rewind()
    position.store(positionBuffer)
    positionBuffer.rewind()
    AL10.alListener(AL10.AL_POSITION, positionBuffer)
  }

  def up: Vector3f = {
    orientationBuffer.position(3)
    val ret = new Vector3f
    ret.load(orientationBuffer)
    ret
  }

  def orientation: Vector3f = {
    orientationBuffer.rewind()
    val ret = new Vector3f
    ret.load(orientationBuffer)
    ret
  }
  def setOrientation(orientation: Vector3f, up: Vector3f): Unit = {
    orientationBuffer.rewind()
    orientation.store(orientationBuffer)
    up.store(orientationBuffer)
    orientationBuffer.rewind()
    AL10.alListener(AL10.AL_ORIENTATION, orientationBuffer)
  }
}