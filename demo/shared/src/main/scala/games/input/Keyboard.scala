package games.input

import java.io.Closeable

object Keyboard {
  private[games]type KeyMapper[T] = BiMap[Key, T]
}

object `package` {
  type Key = Int
}

object Key {
  final val Space: Key = 1
  final val Apostrophe: Key = 2
  final val Circumflex: Key = 3
  final val Comma: Key = 4
  final val Period: Key = 5
  final val Minus: Key = 6
  final val Slash: Key = 7
  final val SemiColon: Key = 8
  final val Equal: Key = 9
  final val BracketLeft: Key = 10
  final val BracketRight: Key = 11
  final val BackSlash: Key = 12
  final val GraveAccent: Key = 13
  final val Escape: Key = 14
  final val Enter: Key = 15
  final val Tab: Key = 16
  final val BackSpace: Key = 17
  final val Insert: Key = 18
  final val Delete: Key = 19
  final val Right: Key = 20
  final val Left: Key = 21
  final val Down: Key = 22
  final val Up: Key = 23
  final val PageUp: Key = 24
  final val PageDown: Key = 25
  final val Home: Key = 26
  final val End: Key = 27
  final val CapsLock: Key = 28
  final val ScrollLock: Key = 29
  final val NumLock: Key = 30
  final val PrintScreen: Key = 31
  final val Pause: Key = 32
  final val N0: Key = 100
  final val N1: Key = 101
  final val N2: Key = 102
  final val N3: Key = 103
  final val N4: Key = 104
  final val N5: Key = 105
  final val N6: Key = 106
  final val N7: Key = 107
  final val N8: Key = 108
  final val N9: Key = 109
  final val A: Key = 200
  final val B: Key = 201
  final val C: Key = 202
  final val D: Key = 203
  final val E: Key = 204
  final val F: Key = 205
  final val G: Key = 206
  final val H: Key = 207
  final val I: Key = 208
  final val J: Key = 209
  final val K: Key = 210
  final val L: Key = 211
  final val M: Key = 212
  final val N: Key = 213
  final val O: Key = 214
  final val P: Key = 215
  final val Q: Key = 216
  final val R: Key = 217
  final val S: Key = 218
  final val T: Key = 219
  final val U: Key = 220
  final val V: Key = 221
  final val W: Key = 222
  final val X: Key = 223
  final val Y: Key = 224
  final val Z: Key = 225
  final val F1: Key = 300
  final val F2: Key = 301
  final val F3: Key = 302
  final val F4: Key = 303
  final val F5: Key = 304
  final val F6: Key = 305
  final val F7: Key = 306
  final val F8: Key = 307
  final val F9: Key = 308
  final val F10: Key = 309
  final val F11: Key = 310
  final val F12: Key = 311
  final val F13: Key = 312
  final val F14: Key = 313
  final val F15: Key = 314
  final val F16: Key = 315
  final val F17: Key = 316
  final val F18: Key = 317
  final val F19: Key = 318
  final val F20: Key = 319
  final val F21: Key = 320
  final val F22: Key = 321
  final val F23: Key = 322
  final val F24: Key = 323
  final val F25: Key = 324
  final val Num0: Key = 400
  final val Num1: Key = 401
  final val Num2: Key = 402
  final val Num3: Key = 403
  final val Num4: Key = 404
  final val Num5: Key = 405
  final val Num6: Key = 406
  final val Num7: Key = 407
  final val Num8: Key = 408
  final val Num9: Key = 409
  final val NumDecimal: Key = 410
  final val NumDivide: Key = 411
  final val NumMultiply: Key = 412
  final val NumSubstract: Key = 413
  final val NumAdd: Key = 414
  final val NumEnter: Key = 415
  final val NumEqual: Key = 416
  final val ShiftLeft: Key = 500
  final val ShiftRight: Key = 501
  final val ControlLeft: Key = 502
  final val ControlRight: Key = 503
  final val AltLeft: Key = 504
  final val AltRight: Key = 505
  final val SuperLeft: Key = 506
  final val SuperRight: Key = 507
  final val MenuLeft: Key = 508
  final val MenuRight: Key = 509
}

case class KeyboardEvent(key: Key, down: Boolean)

abstract class Keyboard extends Closeable {
  def isKeyDown(key: Key): Boolean
  def nextEvent(): Option[KeyboardEvent]

  def close(): Unit = {}
}
