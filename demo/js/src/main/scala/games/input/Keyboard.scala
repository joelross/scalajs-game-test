package games.input

import scala.scalajs.js
import org.scalajs.dom

import scala.collection.mutable.Queue
import scala.collection.mutable.Set

object KeyboardJS {
  val keyCodeMapper = new Keyboard.KeyMapper[Int](
    (Key.Space, 32),
    (Key.Apostrophe, 219), // Chrome
    (Key.Apostrophe, 222), // Firefox
    //(Key.Circumflex, 229), // buggy on Chrome, unsupported on Firefox 
    (Key.Comma, 188),
    (Key.Period, 190),
    (Key.Minus, 189), // Chrome
    (Key.Minus, 173), // Firefox
    (Key.Slash, 191), // According to Oryol
    (Key.N0, 48),
    (Key.N1, 49),
    (Key.N2, 50),
    (Key.N3, 51),
    (Key.N4, 52),
    (Key.N5, 53),
    (Key.N6, 54),
    (Key.N7, 55),
    (Key.N8, 56),
    (Key.N9, 57),
    (Key.SemiColon, 59), // According to Oryol
    (Key.Equal, 64), // According to Oryol
    (Key.A, 65),
    (Key.B, 66),
    (Key.C, 67),
    (Key.D, 68),
    (Key.E, 69),
    (Key.F, 70),
    (Key.G, 71),
    (Key.H, 72),
    (Key.I, 73),
    (Key.J, 74),
    (Key.K, 75),
    (Key.L, 76),
    (Key.M, 77),
    (Key.N, 78),
    (Key.O, 79),
    (Key.P, 80),
    (Key.Q, 81),
    (Key.R, 82),
    (Key.S, 83),
    (Key.T, 84),
    (Key.U, 85),
    (Key.V, 86),
    (Key.W, 87),
    (Key.X, 88),
    (Key.Y, 89),
    (Key.Z, 90),
    (Key.BracketLeft, 219), // According to Oryol
    (Key.BracketRight, 221), // According to Oryol
    (Key.BackSlash, 220), // According to Oryol
    (Key.GraveAccent, 192),
    (Key.Escape, 27),
    (Key.Enter, 13),
    (Key.Tab, 9),
    (Key.BackSpace, 8),
    (Key.Insert, 45),
    (Key.Delete, 46),
    (Key.Right, 39),
    (Key.Left, 37),
    (Key.Down, 40),
    (Key.Up, 38),
    (Key.PageUp, 33),
    (Key.PageDown, 34),
    (Key.Home, 36),
    (Key.End, 35),
    (Key.CapsLock, 20),
    (Key.ScrollLock, 145),
    (Key.NumLock, 144),
    //(Key.PrintScreen, 777), // Doesn't reach the browser (both Linux/Windows)
    (Key.Pause, 19),
    (Key.F1, 112),
    (Key.F2, 113),
    (Key.F3, 114),
    (Key.F4, 115),
    (Key.F5, 116),
    (Key.F6, 117),
    (Key.F7, 118),
    (Key.F8, 119),
    (Key.F9, 120),
    (Key.F10, 121),
    (Key.F11, 122),
    (Key.F12, 123),
    // Unable to test F13 to F25
    /*(Key.F13, 777),
    (Key.F14, 777),
    (Key.F15, 777),
    (Key.F16, 777),
    (Key.F17, 777),
    (Key.F18, 777),
    (Key.F19, 777),
    (Key.F20, 777),
    (Key.F21, 777),
    (Key.F22, 777),
    (Key.F23, 777),
    (Key.F24, 777),
    (Key.F25, 777),*/
    (Key.Num0, 96),
    (Key.Num1, 97),
    (Key.Num2, 98),
    (Key.Num3, 99),
    (Key.Num4, 100),
    (Key.Num5, 101),
    (Key.Num6, 102),
    (Key.Num7, 103),
    (Key.Num8, 104),
    (Key.Num9, 105),
    (Key.NumDecimal, 110),
    (Key.NumDivide, 111),
    (Key.NumMultiply, 106),
    (Key.NumSubstract, 109),
    (Key.NumAdd, 107),
    //(Key.NumEnter, 13), // Duplicate keyCode with key.Enter
    //(Key.NumEqual, 777),
    (Key.ShiftLeft, 16), // location=1
    (Key.ShiftRight, 16), // location=2
    (Key.ControlLeft, 17), // location=1
    (Key.ControlRight, 17), // location=2
    (Key.AltLeft, 18), // location=1
    (Key.AltRight, 18), // location=2 // not able to test
    //(Key.AltGrLeft, 225) // no location (reported by firefox?)
    (Key.SuperLeft, 91), // location=1 // 224 according to oryol
    (Key.SuperRight, 91) // location=2
    //(Key.MenuLeft, 777), // not able to test
    //(Key.MenuRight, 777) // not able to test
    )
}

class KeyboardJS(element: js.Dynamic) extends Keyboard {
  def this() = this(dom.document.asInstanceOf[js.Dynamic])
  def this(html: dom.raw.HTMLElement) = this(html.asInstanceOf[js.Dynamic])

  private val eventQueue: Queue[KeyboardEvent] = Queue()
  private val downKeys: Set[Key] = Set()

  private def selectLocatedKey(leftKey: Key, rightKey: Key, location: Int) = location match {
    case 1 => leftKey
    case 2 => rightKey
    case x => {
      println("Unknown location " + x + " for key " + leftKey + " or " + rightKey)
      leftKey // just default to the left one
    }
  }

  private def locateKeyIfNecessary(key: Key, ev: dom.raw.KeyboardEvent): Key = key match {
    case Key.ShiftLeft | Key.ShiftRight     => selectLocatedKey(Key.ShiftLeft, Key.ShiftRight, ev.location)
    case Key.ControlLeft | Key.ControlRight => selectLocatedKey(Key.ControlLeft, Key.ControlRight, ev.location)
    case Key.AltLeft | Key.AltRight         => selectLocatedKey(Key.AltLeft, Key.AltRight, ev.location)
    case Key.SuperLeft | Key.SuperLeft      => selectLocatedKey(Key.SuperLeft, Key.SuperLeft, ev.location)
    case _                                  => key
  }

  private def keyFromEvent(ev: dom.raw.KeyboardEvent): Option[Key] = {
    val keyCode = ev.keyCode
    KeyboardJS.keyCodeMapper.getForRemote(keyCode) match {
      case Some(key) => Some(locateKeyIfNecessary(key, ev))
      case None      => None // unknown keyCode
    }
  }

  private def keyDown(key: Key): Unit = {
    if (!this.isKeyDown(key)) { // Accept this event only if the key was not yet recognized as "down"
      downKeys += key
      eventQueue += KeyboardEvent(key, true)
    }
  }

  private def keyUp(key: Key): Unit = {
    if (this.isKeyDown(key)) { // Accept this event only if the key was not yet recognized as "up"
      downKeys -= key
      eventQueue += KeyboardEvent(key, false)
    }
  }

  private val onKeyUp = (e: dom.raw.Event) => {
    e.preventDefault()

    val ev = e.asInstanceOf[dom.raw.KeyboardEvent]
    keyFromEvent(ev) match {
      case Some(key) => keyUp(key)
      case None      => // unknown key, ignore
    }
  }
  private val onKeyDown = (e: dom.raw.Event) => {
    e.preventDefault()

    val ev = e.asInstanceOf[dom.raw.KeyboardEvent]
    keyFromEvent(ev) match {
      case Some(key) => keyDown(key)
      case None      => // unknown key, ignore
    }
  }

  element.addEventListener("keyup", onKeyUp, true)
  element.addEventListener("keydown", onKeyDown, true)

  override def close(): Unit = {
    element.removeEventListener("keyup", onKeyUp, true)
    element.removeEventListener("keydown", onKeyDown, true)
  }

  def isKeyDown(key: games.input.Key): Boolean = {
    downKeys.contains(key)
  }

  def nextEvent(): Option[games.input.KeyboardEvent] = {
    if (eventQueue.nonEmpty) Some(eventQueue.dequeue())
    else None
  }
}