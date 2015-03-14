package games.input

import org.lwjgl.input.{ Keyboard => LWJGLKey }

object KeyboardLWJGL {
  val mapper = new KeyMapper[Int](
    (Key.Space, LWJGLKey.KEY_SPACE),
    (Key.Apostrophe, LWJGLKey.KEY_APOSTROPHE),
    //(Key.Circumflex, LWJGLKey.KEY_CIRCUMFLEX), // seems buggy
    (Key.Comma, LWJGLKey.KEY_COMMA),
    (Key.Period, LWJGLKey.KEY_PERIOD),
    (Key.Minus, LWJGLKey.KEY_MINUS),
    (Key.Slash, LWJGLKey.KEY_SLASH),
    (Key.N0, LWJGLKey.KEY_0),
    (Key.N1, LWJGLKey.KEY_1),
    (Key.N2, LWJGLKey.KEY_2),
    (Key.N3, LWJGLKey.KEY_3),
    (Key.N4, LWJGLKey.KEY_4),
    (Key.N5, LWJGLKey.KEY_5),
    (Key.N6, LWJGLKey.KEY_6),
    (Key.N7, LWJGLKey.KEY_7),
    (Key.N8, LWJGLKey.KEY_8),
    (Key.N9, LWJGLKey.KEY_9),
    (Key.SemiColon, LWJGLKey.KEY_SEMICOLON),
    (Key.Equal, LWJGLKey.KEY_EQUALS),
    (Key.A, LWJGLKey.KEY_A),
    (Key.B, LWJGLKey.KEY_B),
    (Key.C, LWJGLKey.KEY_C),
    (Key.D, LWJGLKey.KEY_D),
    (Key.E, LWJGLKey.KEY_E),
    (Key.F, LWJGLKey.KEY_F),
    (Key.G, LWJGLKey.KEY_G),
    (Key.H, LWJGLKey.KEY_H),
    (Key.I, LWJGLKey.KEY_I),
    (Key.J, LWJGLKey.KEY_J),
    (Key.K, LWJGLKey.KEY_K),
    (Key.L, LWJGLKey.KEY_L),
    (Key.M, LWJGLKey.KEY_M),
    (Key.N, LWJGLKey.KEY_N),
    (Key.O, LWJGLKey.KEY_O),
    (Key.P, LWJGLKey.KEY_P),
    (Key.Q, LWJGLKey.KEY_Q),
    (Key.R, LWJGLKey.KEY_R),
    (Key.S, LWJGLKey.KEY_S),
    (Key.T, LWJGLKey.KEY_T),
    (Key.U, LWJGLKey.KEY_U),
    (Key.V, LWJGLKey.KEY_V),
    (Key.W, LWJGLKey.KEY_W),
    (Key.X, LWJGLKey.KEY_X),
    (Key.Y, LWJGLKey.KEY_Y),
    (Key.Z, LWJGLKey.KEY_Z),
    (Key.BracketLeft, LWJGLKey.KEY_LBRACKET),
    (Key.BracketRight, LWJGLKey.KEY_RBRACKET),
    (Key.BackSlash, LWJGLKey.KEY_BACKSLASH),
    (Key.GraveAccent, LWJGLKey.KEY_GRAVE),
    (Key.Escape, LWJGLKey.KEY_ESCAPE),
    (Key.Enter, LWJGLKey.KEY_RETURN),
    (Key.Tab, LWJGLKey.KEY_TAB),
    (Key.BackSpace, LWJGLKey.KEY_BACK),
    (Key.Insert, LWJGLKey.KEY_INSERT),
    (Key.Delete, LWJGLKey.KEY_DELETE),
    (Key.Right, LWJGLKey.KEY_RIGHT),
    (Key.Left, LWJGLKey.KEY_LEFT),
    (Key.Down, LWJGLKey.KEY_DOWN),
    (Key.Up, LWJGLKey.KEY_UP),
    (Key.PageUp, LWJGLKey.KEY_PRIOR),
    (Key.PageDown, LWJGLKey.KEY_NEXT),
    (Key.Home, LWJGLKey.KEY_HOME),
    (Key.End, LWJGLKey.KEY_END),
    (Key.CapsLock, LWJGLKey.KEY_CAPITAL),
    (Key.ScrollLock, LWJGLKey.KEY_SCROLL),
    (Key.NumLock, LWJGLKey.KEY_NUMLOCK),
    (Key.PrintScreen, LWJGLKey.KEY_SYSRQ),
    (Key.Pause, LWJGLKey.KEY_PAUSE),
    (Key.F1, LWJGLKey.KEY_F1),
    (Key.F2, LWJGLKey.KEY_F2),
    (Key.F3, LWJGLKey.KEY_F3),
    (Key.F4, LWJGLKey.KEY_F4),
    (Key.F5, LWJGLKey.KEY_F5),
    (Key.F6, LWJGLKey.KEY_F6),
    (Key.F7, LWJGLKey.KEY_F7),
    (Key.F8, LWJGLKey.KEY_F8),
    (Key.F9, LWJGLKey.KEY_F9),
    (Key.F10, LWJGLKey.KEY_F10),
    (Key.F11, LWJGLKey.KEY_F11),
    (Key.F12, LWJGLKey.KEY_F12),
    (Key.F13, LWJGLKey.KEY_F13),
    (Key.F14, LWJGLKey.KEY_F14),
    (Key.F15, LWJGLKey.KEY_F15),
    (Key.F16, LWJGLKey.KEY_F16),
    (Key.F17, LWJGLKey.KEY_F17),
    (Key.F18, LWJGLKey.KEY_F18),
    (Key.F19, LWJGLKey.KEY_F19),
    // Nothing for F20 to F25
    (Key.Num0, LWJGLKey.KEY_NUMPAD0),
    (Key.Num1, LWJGLKey.KEY_NUMPAD1),
    (Key.Num2, LWJGLKey.KEY_NUMPAD2),
    (Key.Num3, LWJGLKey.KEY_NUMPAD3),
    (Key.Num4, LWJGLKey.KEY_NUMPAD4),
    (Key.Num5, LWJGLKey.KEY_NUMPAD5),
    (Key.Num6, LWJGLKey.KEY_NUMPAD6),
    (Key.Num7, LWJGLKey.KEY_NUMPAD7),
    (Key.Num8, LWJGLKey.KEY_NUMPAD8),
    (Key.Num9, LWJGLKey.KEY_NUMPAD9),
    (Key.NumDecimal, LWJGLKey.KEY_DECIMAL),
    (Key.NumDivide, LWJGLKey.KEY_DIVIDE),
    (Key.NumMultiply, LWJGLKey.KEY_MULTIPLY),
    (Key.NumSubstract, LWJGLKey.KEY_SUBTRACT),
    (Key.NumAdd, LWJGLKey.KEY_ADD),
    (Key.NumEnter, LWJGLKey.KEY_NUMPADENTER),
    (Key.NumEqual, LWJGLKey.KEY_NUMPADEQUALS),
    (Key.ShiftLeft, LWJGLKey.KEY_LSHIFT),
    (Key.ShiftRight, LWJGLKey.KEY_RSHIFT),
    (Key.ControlLeft, LWJGLKey.KEY_LCONTROL),
    (Key.ControlRight, LWJGLKey.KEY_RCONTROL),
    (Key.AltLeft, LWJGLKey.KEY_LMENU),
    (Key.AltRight, LWJGLKey.KEY_RMENU),
    (Key.SuperLeft, LWJGLKey.KEY_LMETA),
    (Key.SuperRight, LWJGLKey.KEY_RMETA)
    //(Key.MenuLeft, 184 ???),
    //(Key.MenuRight, ???),
    )
}

class KeyboardLWJGL() extends Keyboard {
  LWJGLKey.create()

  override def close(): Unit = {
    LWJGLKey.destroy()
  }

  def isKeyDown(key: games.input.Key): Boolean = {
    LWJGLKey.poll()
    KeyboardLWJGL.mapper.getForLocal(key) match {
      case Some(lwjglKeyCode) => LWJGLKey.isKeyDown(lwjglKeyCode)
      case None               => false // unsupported key
    }
  }

  def nextEvent(): Option[games.input.KeyboardEvent] = {
    if (LWJGLKey.next()) {
      val keyCode = LWJGLKey.getEventKey
      KeyboardLWJGL.mapper.getForRemote(keyCode) match {
        case Some(key) => {
          val down = LWJGLKey.getEventKeyState
          Some(KeyboardEvent(key, down))
        }
        case None => {
          nextEvent() // unsupported key, skip to the next event
        }
      }
    } else {
      None
    }
  }
}