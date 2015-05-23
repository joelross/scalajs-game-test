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
