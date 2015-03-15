package games.input

import java.io.Closeable

sealed abstract class Key

object Keyboard {
  private[games] type KeyMapper[T] = BiMap[Key, T]
}

object Key {
  case object Space extends Key
  case object Apostrophe extends Key
  case object Circumflex extends Key
  case object Comma extends Key
  case object Period extends Key
  case object Minus extends Key
  case object Slash extends Key
  case object N0 extends Key
  case object N1 extends Key
  case object N2 extends Key
  case object N3 extends Key
  case object N4 extends Key
  case object N5 extends Key
  case object N6 extends Key
  case object N7 extends Key
  case object N8 extends Key
  case object N9 extends Key
  case object SemiColon extends Key
  case object Equal extends Key
  case object A extends Key
  case object B extends Key
  case object C extends Key
  case object D extends Key
  case object E extends Key
  case object F extends Key
  case object G extends Key
  case object H extends Key
  case object I extends Key
  case object J extends Key
  case object K extends Key
  case object L extends Key
  case object M extends Key
  case object N extends Key
  case object O extends Key
  case object P extends Key
  case object Q extends Key
  case object R extends Key
  case object S extends Key
  case object T extends Key
  case object U extends Key
  case object V extends Key
  case object W extends Key
  case object X extends Key
  case object Y extends Key
  case object Z extends Key
  case object BracketLeft extends Key
  case object BracketRight extends Key
  case object BackSlash extends Key
  case object GraveAccent extends Key
  case object Escape extends Key
  case object Enter extends Key
  case object Tab extends Key
  case object BackSpace extends Key
  case object Insert extends Key
  case object Delete extends Key
  case object Right extends Key
  case object Left extends Key
  case object Down extends Key
  case object Up extends Key
  case object PageUp extends Key
  case object PageDown extends Key
  case object Home extends Key
  case object End extends Key
  case object CapsLock extends Key
  case object ScrollLock extends Key
  case object NumLock extends Key
  case object PrintScreen extends Key
  case object Pause extends Key
  case object F1 extends Key
  case object F2 extends Key
  case object F3 extends Key
  case object F4 extends Key
  case object F5 extends Key
  case object F6 extends Key
  case object F7 extends Key
  case object F8 extends Key
  case object F9 extends Key
  case object F10 extends Key
  case object F11 extends Key
  case object F12 extends Key
  case object F13 extends Key
  case object F14 extends Key
  case object F15 extends Key
  case object F16 extends Key
  case object F17 extends Key
  case object F18 extends Key
  case object F19 extends Key
  case object F20 extends Key
  case object F21 extends Key
  case object F22 extends Key
  case object F23 extends Key
  case object F24 extends Key
  case object F25 extends Key
  case object Num0 extends Key
  case object Num1 extends Key
  case object Num2 extends Key
  case object Num3 extends Key
  case object Num4 extends Key
  case object Num5 extends Key
  case object Num6 extends Key
  case object Num7 extends Key
  case object Num8 extends Key
  case object Num9 extends Key
  case object NumDecimal extends Key
  case object NumDivide extends Key
  case object NumMultiply extends Key
  case object NumSubstract extends Key
  case object NumAdd extends Key
  case object NumEnter extends Key
  case object NumEqual extends Key
  case object ShiftLeft extends Key
  case object ShiftRight extends Key
  case object ControlLeft extends Key
  case object ControlRight extends Key
  case object AltLeft extends Key
  case object AltRight extends Key
  case object SuperLeft extends Key
  case object SuperRight extends Key
  case object MenuLeft extends Key
  case object MenuRight extends Key
}

case class KeyboardEvent(key: Key, down: Boolean)

abstract class Keyboard extends Closeable {
  def isKeyDown(key: Key): Boolean
  def nextEvent(): Option[KeyboardEvent]

  def close(): Unit = {}
}