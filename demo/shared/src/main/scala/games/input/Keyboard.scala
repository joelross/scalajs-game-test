package games.input

import java.io.Closeable

object Keyboard {
  private[games]type KeyMapper[T] = BiMap[Key, T]
}

object Key {
  final val Space = 1
  final val Apostrophe = 2
  final val Circumflex = 3
  final val Comma = 4
  final val Period = 5
  final val Minus = 6
  final val Slash = 7
  final val SemiColon = 8
  final val Equal = 9
  final val BracketLeft = 10
  final val BracketRight = 11
  final val BackSlash = 12
  final val GraveAccent = 13
  final val Escape = 14
  final val Enter = 15
  final val Tab = 16
  final val BackSpace = 17
  final val Insert = 18
  final val Delete = 19
  final val Right = 20
  final val Left = 21
  final val Down = 22
  final val Up = 23
  final val PageUp = 24
  final val PageDown = 25
  final val Home = 26
  final val End = 27
  final val CapsLock = 28
  final val ScrollLock = 29
  final val NumLock = 30
  final val PrintScreen = 31
  final val Pause = 32
  final val N0 = 100
  final val N1 = 101
  final val N2 = 102
  final val N3 = 103
  final val N4 = 104
  final val N5 = 105
  final val N6 = 106
  final val N7 = 107
  final val N8 = 108
  final val N9 = 109
  final val A = 200
  final val B = 201
  final val C = 202
  final val D = 203
  final val E = 204
  final val F = 205
  final val G = 206
  final val H = 207
  final val I = 208
  final val J = 209
  final val K = 210
  final val L = 211
  final val M = 212
  final val N = 213
  final val O = 214
  final val P = 215
  final val Q = 216
  final val R = 217
  final val S = 218
  final val T = 219
  final val U = 220
  final val V = 221
  final val W = 222
  final val X = 223
  final val Y = 224
  final val Z = 225
  final val F1 = 300
  final val F2 = 301
  final val F3 = 302
  final val F4 = 303
  final val F5 = 304
  final val F6 = 305
  final val F7 = 306
  final val F8 = 307
  final val F9 = 308
  final val F10 = 309
  final val F11 = 310
  final val F12 = 311
  final val F13 = 312
  final val F14 = 313
  final val F15 = 314
  final val F16 = 315
  final val F17 = 316
  final val F18 = 317
  final val F19 = 318
  final val F20 = 319
  final val F21 = 320
  final val F22 = 321
  final val F23 = 322
  final val F24 = 323
  final val F25 = 324
  final val Num0 = 400
  final val Num1 = 401
  final val Num2 = 402
  final val Num3 = 403
  final val Num4 = 404
  final val Num5 = 405
  final val Num6 = 406
  final val Num7 = 407
  final val Num8 = 408
  final val Num9 = 409
  final val NumDecimal = 410
  final val NumDivide = 411
  final val NumMultiply = 412
  final val NumSubstract = 413
  final val NumAdd = 414
  final val NumEnter = 415
  final val NumEqual = 416
  final val ShiftLeft = 500
  final val ShiftRight = 501
  final val ControlLeft = 502
  final val ControlRight = 503
  final val AltLeft = 504
  final val AltRight = 505
  final val SuperLeft = 506
  final val SuperRight = 507
  final val MenuLeft = 508
  final val MenuRight = 509
}

case class KeyboardEvent(key: Key, down: Boolean)

abstract class Keyboard extends Closeable {
  def isKeyDown(key: Key): Boolean
  def nextEvent(): Option[KeyboardEvent]

  def close(): Unit = {}
}