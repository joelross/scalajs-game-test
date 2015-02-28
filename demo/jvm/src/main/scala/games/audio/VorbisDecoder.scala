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

class VorbisDecoder private[games] (in: InputStream) extends FilterInputStream(in) {
  private val packet = new Packet
  private val page = new Page
  private val streamState = new StreamState
  private val syncState = new SyncState

  private val dspState = new DspState
  private val block = new Block(dspState)
  private val comment = new Comment
  private val info = new Info

  private val bufferSize = 4096

  private var firstPage = true

  private def getNextPage(): Page = syncState.pageout(page) match {
    case 0 => { // need more data
      val index = syncState.buffer(bufferSize)
      val buffer = syncState.data
      var read = in.read(buffer, index, bufferSize)
      if (read < 0) throw new EOFException()
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
      page
    }
    case x => throw new RuntimeException("Could not retrieve page from buffer. Code " + x)
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
    } catch {
      case e: Exception => throw new RuntimeException("Could not init the decoder", e)
    }
  }

  def rate: Int = info.rate
  def channels: Int = info.channels
  
  override def available(): Int = ???
  override def close(): Unit = ???
  override def mark(readLimit: Int): Unit = throw new IOException("Mark not supported")
  override def markSupported(): Boolean = false
  override def read(): Int = ???
  override def read(b: Array[Byte]): Int = this.read(b, 0, b.length)
  override def read(b: Array[Byte], off: Int, len: Int): Int = ???
  override def reset(): Unit = throw new IOException("Reset not supported")
  override def skip(n: Long): Long = ???
}