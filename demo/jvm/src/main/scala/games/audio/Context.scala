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

  def prepareBufferedData(res: games.Resource): scala.concurrent.Future[games.audio.BufferedData] = ???
  def prepareRawData(data: java.nio.ByteBuffer, format: games.audio.Format, channels: Int, freq: Int): scala.concurrent.Future[games.audio.BufferedData] = ???
  def prepareStreamingData(res: games.Resource): scala.concurrent.Future[games.audio.Data] = ???

  def createSource(): games.audio.Source = ???
  def createSource3D(): games.audio.Source3D = ???

  val listener: games.audio.Listener = new ALListener()

  def volume: Float = masterVolume
  def volume_=(volume: Float): Unit = {
    masterVolume = volume
    sources.foreach { source =>
      //source.masterVolumeChanged()
      ??? // TODO
    }
  }

  private[games] var masterVolume = 1f

  override def close(): Unit = {
    super.close()
    AL.destroy()
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