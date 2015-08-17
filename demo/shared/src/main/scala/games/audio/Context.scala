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
  def prepareStreamingData(res: games.Resource): Future[games.audio.Data]
  def prepareBufferedData(res: games.Resource): Future[games.audio.BufferedData]
  def prepareRawData(data: java.nio.ByteBuffer, format: games.audio.Format, channels: Int, freq: Int): Future[games.audio.BufferedData]

  private def tryFutures[T](res: TraversableOnce[games.Resource], fun: Resource => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    val promise = Promise[T]

    val iterator = res.toIterator

    def tryNext(): Unit = if (iterator.hasNext) {
      val nextResource = iterator.next()
      val dataFuture = fun(nextResource)
      dataFuture.onSuccess { case v => promise.success(v) }
      dataFuture.onFailure { case t => tryNext() }
    } else {
      promise.failure(new RuntimeException("No usable resource in " + res))
    }

    tryNext()

    promise.future
  }

  def prepareStreamingData(res: scala.collection.TraversableOnce[games.Resource])(implicit ec: scala.concurrent.ExecutionContext): Future[games.audio.Data] = tryFutures(res, prepareStreamingData(_))
  def prepareBufferedData(res: scala.collection.TraversableOnce[games.Resource])(implicit ec: scala.concurrent.ExecutionContext): Future[games.audio.BufferedData] = tryFutures(res, prepareBufferedData(_))

  def createSource(): games.audio.Source
  def createSource3D(): games.audio.Source3D

  def listener: games.audio.Listener

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
  def position: games.math.Vector3f
  def position_=(position: games.math.Vector3f)
}

abstract class Listener extends Closeable with Spatial {
  def up: games.math.Vector3f

  def orientation: games.math.Vector3f

  def setOrientation(orientation: games.math.Vector3f, up: games.math.Vector3f): Unit

  def close(): Unit = {}
}

abstract class Data extends Closeable {
  def attach(source: games.audio.AbstractSource): scala.concurrent.Future[games.audio.Player]

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
  def attachNow(source: games.audio.AbstractSource): games.audio.Player
  def attach(source: games.audio.AbstractSource): scala.concurrent.Future[games.audio.Player] = try {
    Future.successful(this.attachNow(source))
  } catch {
    case t: Throwable => Future.failed(t)
  }
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
