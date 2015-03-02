package games.audio

import java.nio.ByteBuffer

abstract class Converter {
  def apply(sample: Array[Float], dst: ByteBuffer): Unit
  def hasEnoughSpace(channels: Int, dst: ByteBuffer): Boolean
  def bytePerValue: Int
}

class Fixed8Converter extends Converter {
  def apply(sample: Array[Float], dst: ByteBuffer): Unit = ???
  def hasEnoughSpace(channels: Int, dst: ByteBuffer): Boolean = ???
  val bytePerValue = 1
}

class Fixed16Converter extends Converter {
  def apply(sample: Array[Float], dst: ByteBuffer): Unit = {
    def convChannel(value: Float) {
      val amplified = (value * 32767).toInt
      val clamped = Math.max(Short.MinValue, Math.min(Short.MaxValue, amplified)).toShort
      dst.putShort(clamped)
    }

    var channelNo = 0
    while (channelNo < sample.length) {
      convChannel(sample(channelNo))
      channelNo += 1
    }
  }
  def hasEnoughSpace(channels: Int, dst: ByteBuffer): Boolean = dst.remaining() >= channels * 2
  val bytePerValue = 2
}

class Floating32Converter extends Converter {
  def apply(sample: Array[Float], dst: ByteBuffer): Unit = ???
  def hasEnoughSpace(channels: Int, dst: ByteBuffer): Boolean = ???
  val bytePerValue = 4
}