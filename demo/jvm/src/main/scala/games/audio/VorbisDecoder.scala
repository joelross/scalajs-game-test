package games.audio

import java.io.InputStream
import com.jcraft.jogg.Packet
import com.jcraft.jogg.Page
import com.jcraft.jogg.StreamState
import com.jcraft.jogg.SyncState
import com.jcraft.jorbis.DspState
import com.jcraft.jorbis.Block
import com.jcraft.jorbis.Info
import com.jcraft.jorbis.Comment
import java.io.EOFException
import java.io.FilterInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VorbisDecoder private[games] (in: InputStream, conv: Converter) {
  private val packet = new Packet
  private val page = new Page
  private val streamState = new StreamState
  private val syncState = new SyncState

  private val dspState = new DspState
  private val block = new Block(dspState)
  private val comment = new Comment
  private val info = new Info

  private var firstPage = true
  private var lastPage = false

  private val readBufferSize = 4096
  //private val convBufferSize = 4096

  private def getNextPage(): Page = {
    syncState.pageout(page) match {
      case 0 => { // need more data
        val index = syncState.buffer(readBufferSize)
        val buffer = syncState.data
        var read = in.read(buffer, index, readBufferSize)
        if (read < 0) {
          if (!lastPage) { System.err.println("Warning: End of stream reached before EOS page") }
          throw new EOFException()
        }
        val code = syncState.wrote(read)
        if (code < 0) throw new RuntimeException("Could not load the buffer. Code " + code)
        else getNextPage() // once the buffer is loaded successfully, try again
      }
      case 1 => { // page ok
        if (firstPage) {
          firstPage = false
          streamState.init(page.serialno())
          val code = streamState.reset()
          if (code < 0) throw new RuntimeException("Could not reset streamState. Code " + code)

          info.init()
          comment.init()
        }
        if (lastPage) System.err.println("Warning: EOS page already reached")
        else lastPage = page.eos() != 0
        page
      }
      case x => throw new RuntimeException("Could not retrieve page from buffer. Code " + x)
    }
  }

  def getNextPacket(): Packet = streamState.packetout(packet) match {
    case 0 => { // need a new page
      val code = streamState.pagein(getNextPage())
      if (code < 0) throw new RuntimeException("Could not load the page. Code " + code)
      else getNextPacket() // once a new page is loaded successfully, try again
    }
    case 1 => packet // packet ok
    case x => throw new RuntimeException("Could not retrieve packet from page. Code " + x)
  }

  init()

  private def init() {
    try {
      syncState.init()

      for (i <- 1 to 3) { // Decode the three header packets
        val code = info.synthesis_headerin(comment, getNextPacket())
        if (code < 0) throw new RuntimeException("Could not synthesize the info. Code " + code)
      }

      if (dspState.synthesis_init(info) < 0) throw new RuntimeException("Could not init DspState")
      block.init(dspState)

      pcmIn = new Array[Array[Array[Float]]](1)
      indexIn = new Array[Int](info.channels)
      sampleTmp = new Array[Float](info.channels)
      //convBuffer = ByteBuffer.allocate(convBufferSize).order(ByteOrder.nativeOrder())
    } catch {
      case e: Exception => throw new RuntimeException("Could not init the decoder", e)
    }
  }

  def rate: Int = info.rate
  def channels: Int = info.channels

  private var pcmIn: Array[Array[Array[Float]]] = _
  private var indexIn: Array[Int] = _
  //private var convBuffer: ByteBuffer = _
  private var remainingSamples = 0
  private var samplesRead = 0

  private var sampleTmp: Array[Float] = _

  private def decodeNextPacket(): Unit = {
    if (dspState.synthesis_read(samplesRead) < 0) throw new RuntimeException("Could not acknowledge read samples")
    samplesRead = 0

    if (block.synthesis(this.getNextPacket()) < 0) throw new RuntimeException("Could not synthesize the block from packet")
    if (dspState.synthesis_blockin(block) < 0) throw new RuntimeException("Could not synthesize dspState from block")

    val availableSamples = dspState.synthesis_pcmout(pcmIn, indexIn)
    if (availableSamples < 0) throw new RuntimeException("Could not decode the block")
    else if (availableSamples == 0) System.err.println("Warning: 0 samples decoded")

    remainingSamples = availableSamples
  }

  def read(out: ByteBuffer): Int = {
    if (remainingSamples <= 0) {
      decodeNextPacket()
    }

    var total = 0

    while (true) {
      if (remainingSamples <= 0 || !conv.hasEnoughSpace(info.channels, out)) return total

      var channelNo = 0
      while (channelNo < info.channels) {
        sampleTmp(channelNo) = pcmIn(0)(channelNo)(indexIn(channelNo) + samplesRead)
        channelNo += 1
      }

      conv(sampleTmp, out)

      samplesRead += 1
      remainingSamples -= 1

      total += 1
    }

    ???
  }

  def readFully(out: ByteBuffer): Int = {
    val cap = out.remaining()

    if (out.remaining() % (info.channels * conv.bytePerValue) != 0) throw new RuntimeException("Buffer capacity not aligned (remaining " + out.remaining() + ", required multiple of " + (info.channels * conv.bytePerValue) + ")")

    var total = 0

    while (out.remaining() > 0) {
      total += read(out)
    }

    if (cap != total * conv.bytePerValue * info.channels) {
      System.err.println("Warning: amount of read data does not match (internal error)")
    }

    total
  }
}