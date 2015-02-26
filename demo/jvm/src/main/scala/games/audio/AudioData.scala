package games.audio

import games.Resource

sealed abstract class AudioData

class BufferedAudioData(res: Resource) extends AudioData {
}

class RawAudioData extends AudioData {

}

class StreamingAudioData(res: Resource) extends AudioData {

}