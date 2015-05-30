package games.audio

import scala.concurrent.{ Promise, Future, ExecutionContext }
import scala.collection.mutable

import games.Resource
import games.math.Vector3f
import java.io.Closeable

import java.nio.ByteBuffer

abstract sealed class Format

object Format {
  case object Float32 extends Format
}

abstract class Context extends Closeable {
  def prepareStreamingData(res: Resource): Future[games.audio.Data]
  def prepareBufferedData(res: Resource): Future[games.audio.BufferedData]
  def prepareRawData(data: ByteBuffer, format: Format, channels: Int, freq: Int): Future[games.audio.BufferedData]

  private def tryFutures[T](res: Seq[Resource], fun: Resource => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    val promise = Promise[T]

    def prep(datas: List[Resource]): Unit = res match {
      case Nil => promise.failure(new RuntimeException("No usable resources in " + res))
      case x :: xs =>
        val dataFuture = fun(x)
        dataFuture.onSuccess { case v => promise.success(v) }
        dataFuture.onFailure { case t => prep(xs) }
    }

    prep(res.toList)

    promise.future
  }

  def prepareStreamingData(res: Seq[Resource])(implicit ec: ExecutionContext): Future[games.audio.Data] = tryFutures(res, prepareStreamingData(_))
  def prepareBufferedData(res: Seq[Resource])(implicit ec: ExecutionContext): Future[games.audio.BufferedData] = tryFutures(res, prepareBufferedData(_))

  def createSource(): games.audio.Source
  def createSource3D(): games.audio.Source3D

  def listener: Listener

  def volume: Float
  def volume_=(volume: Float): Unit

  private[games] val datas: mutable.Set[Data] = mutable.Set()
  private[games] def registerData(data: Data): Unit = datas += data
  private[games] def unregisterData(data: Data): Unit = datas -= data

  private[games] val sources: mutable.Set[AbstractSource] = mutable.Set()
  private[games] def registerSource(source: AbstractSource): Unit = sources += source
  private[games] def unregisterSource(source: AbstractSource): Unit = sources -= source

  def close(): Unit = {
    for (data <- this.datas) {
      data.close()
    }
    for (source <- this.sources) {
      source.close()
    }

    datas.clear()
    sources.clear()
  }
}

sealed trait Spatial {
  def position: Vector3f
  def position_=(position: Vector3f)
}

abstract class Listener extends Closeable with Spatial {
  def up: Vector3f

  def orientation: Vector3f

  def setOrientation(orientation: Vector3f, up: Vector3f): Unit

  def close(): Unit = {}
}

abstract class Data extends Closeable {
  def attach(source: AbstractSource): Future[games.audio.Player]

  private[games] val players: mutable.Set[Player] = mutable.Set()
  private[games] def registerPlayer(player: Player): Unit = players += player
  private[games] def unregisterPlayer(player: Player): Unit = players -= player

  def close(): Unit = {
    for (player <- players) {
      player.close()
    }

    players.clear()
  }
}

abstract class BufferedData extends Data {
  def attachNow(source: AbstractSource): games.audio.Player
}

abstract class Player extends Closeable {
  def playing: Boolean
  def playing_=(playing: Boolean): Unit

  def volume: Float
  def volume_=(volume: Float): Unit

  def loop: Boolean
  def loop_=(loop: Boolean): Unit

  def pitch: Float
  def pitch_=(pitch: Float): Unit

  def close(): Unit = {
    this.playing = false
  }
}

abstract class AbstractSource extends Closeable {
  private[games] val players: mutable.Set[Player] = mutable.Set()
  private[games] def registerPlayer(player: Player): Unit = players += player
  private[games] def unregisterPlayer(player: Player): Unit = players -= player

  def close(): Unit = {
    for (player <- players) {
      player.close()
    }

    players.clear()
  }
}
abstract class Source extends AbstractSource
abstract class Source3D extends AbstractSource with Spatial
