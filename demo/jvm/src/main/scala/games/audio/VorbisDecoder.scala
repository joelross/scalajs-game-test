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

class VorbisDecoder private[games] (in: InputStream) {
  private val packet = new Packet
  private val page = new Page
  private val streamState = new StreamState
  private val syncState = new SyncState

  private val dspState = new DspState
  private val block = new Block(dspState)
  private val comment = new Comment
  val info = new Info

  private val bufferSize = 256

  syncState.init()

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
    case 1 => {
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
      for (i <- 1 to 3) {
        val code = info.synthesis_headerin(comment, getNextPacket())
        if (code < 0) throw new RuntimeException("Could not synthesize the info. Code " + code)
      }
    } catch {
      case t => throw new RuntimeException("Could not init the decoder", t)
    }
  }

  //  init()
  //  
  //  private def init() {
  //    // init JOrbis
  //    val bufferSize = 2048
  //    
  //    syncState.init()
  //    syncState.buffer(bufferSize)
  //    val buffer = syncState.data
  //    
  //    // read header
  //    var needMoreData = true
  //    var packetNo = 1
  //    var count = 0
  //    var index = 0
  //    
  //    def readPage() {
  //      
  //    }
  //    
  //    while(needMoreData) {
  //      count = in.read(buffer, index, bufferSize)
  //      syncState.wrote(count)
  //      packetNo match {
  //        case 1 => {
  //          syncState.pageout(page) match {
  //            case -1 => throw new RuntimeException("There is a hole in the first packet data")
  //            case 0 => // need more data (do nothing?)
  //            case 1 => {
  //              streamState.init(page.serialno())
  //              streamState.reset()
  //              
  //              info.init()
  //              comment.init()
  //              
  //              if(streamState.pagein(page) == -1 || streamState.packetout(packet) != 1 || info.synthesis_headerin(comment, packet) < 0) {
  //                throw new RuntimeException("Error while reading the first header page")
  //              }
  //              
  //              packetNo += 1
  //            }
  //          }
  //        }
  //      }
  //    }
  //  }
}