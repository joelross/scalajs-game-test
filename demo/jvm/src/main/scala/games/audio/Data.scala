package games.audio

import games.JvmUtils
import games.Resource
import org.lwjgl.openal.AL10
import org.lwjgl.openal.Util
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.ByteArrayOutputStream
import java.io.EOFException

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

sealed trait ALAbstractSource extends AbstractSource {
  override def close(): Unit = {
    super.close()
  }
}

class ALSource(val ctx: ALContext) extends Source with ALAbstractSource {
  // TODO

  ctx.registerSource(this)

  override def close(): Unit = {
    super.close()

    ctx.unregisterSource(this)
  }
}

class ALSource3D(val ctx: ALContext) extends Source3D with ALAbstractSource {
  def position: games.math.Vector3f = ???
  def position_=(position: games.math.Vector3f): Unit = ???

  // TODO

  ctx.registerSource(this)

  override def close(): Unit = {
    super.close()

    ctx.unregisterSource(this)
  }
}

sealed trait ALData extends Data {
  override def close(): Unit = {
    super.close()
  }
}

class ALBufferData(val ctx: ALContext) extends BufferedData with ALData {
  ctx.registerData(this)

  def attach(source: games.audio.AbstractSource): scala.concurrent.Future[games.audio.Player] = Future.successful(this.attachNow(source))
  def attachNow(source: games.audio.AbstractSource): games.audio.Player = ???

  override def close(): Unit = {
    super.close()

    ctx.unregisterData(this)
  }
}

class ALStreamingData(val ctx: ALContext) extends Data with ALData {
  ctx.registerData(this)

  def attach(source: games.audio.AbstractSource): scala.concurrent.Future[games.audio.Player] = ???

  override def close(): Unit = {
    super.close()

    ctx.unregisterData(this)
  }
}

sealed trait ALPlayer extends Player

class ALBufferPlayer(val data: ALBufferData, val source: ALAbstractSource) extends ALPlayer {

  source.registerPlayer(this)
  data.registerPlayer(this)

  def loop: Boolean = ???
  def loop_=(loop: Boolean): Unit = ???

  def pitch: Float = ???
  def pitch_=(pitch: Float): Unit = ???

  def playing: Boolean = ???
  def playing_=(playing: Boolean): Unit = ???

  def volume: Float = ???
  def volume_=(volume: Float): Unit = ???

  override def close(): Unit = {
    super.close()

    source.unregisterPlayer(this)
    data.unregisterPlayer(this)
  }
}

class ALStreamingPlayer(val data: ALStreamingData, val source: ALAbstractSource) extends ALPlayer {
  source.registerPlayer(this)
  data.registerPlayer(this)

  def loop: Boolean = ???
  def loop_=(loop: Boolean): Unit = ???

  def pitch: Float = ???
  def pitch_=(pitch: Float): Unit = ???

  def playing: Boolean = ???
  def playing_=(playing: Boolean): Unit = ???

  def volume: Float = ???
  def volume_=(volume: Float): Unit = ???

  override def close(): Unit = {
    super.close()

    source.unregisterPlayer(this)
    data.unregisterPlayer(this)
  }
}
