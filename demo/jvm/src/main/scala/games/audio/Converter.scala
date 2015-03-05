package games.audio

import java.nio.ByteBuffer

abstract class Converter {
  def apply(value: Float, dst: ByteBuffer): Unit
  val bytePerValue: Int
}

class FixedSigned8Converter extends Converter {
  def apply(value: Float, dst: ByteBuffer): Unit = {
    val amplified = (value * Byte.MaxValue).toInt
    val clamped = Math.max(Byte.MinValue, Math.min(Byte.MaxValue, amplified)).toByte
    dst.put(clamped)
  }
  val bytePerValue = 1
}

/**
 * Usable for OpenAL with format AL_FORMAT_MONO8/AL_FORMAT_STEREO8
 */
class FixedUnsigned8Converter extends Converter {
  private val max = 255

  def apply(value: Float, dst: ByteBuffer): Unit = {
    val amplified = ((value + 1) / 2 * max).toInt
    val clamped = Math.max(0, Math.min(max, amplified)).toByte
    dst.put(clamped)
  }
  val bytePerValue = 1
}

/**
 * Usable for OpenAL with format AL_FORMAT_MONO16/AL_FORMAT_STEREO16
 */
class FixedSigned16Converter extends Converter {
  def apply(value: Float, dst: ByteBuffer): Unit = {
    val amplified = (value * Short.MaxValue).toInt
    val clamped = Math.max(Short.MinValue, Math.min(Short.MaxValue, amplified)).toShort
    dst.putShort(clamped)
  }
  val bytePerValue = 2
}

class FixedUnsigned16Converter extends Converter {
  private val max = 65535

  def apply(value: Float, dst: ByteBuffer): Unit = {
    val amplified = ((value + 1) / 2 * max).toInt
    val clamped = Math.max(0, Math.min(max, amplified)).toShort
    dst.putShort(clamped)
  }
  val bytePerValue = 2
}

class Floating32Converter extends Converter {
  def apply(value: Float, dst: ByteBuffer): Unit = {
    dst.putFloat(value)
  }
  val bytePerValue = 4
}