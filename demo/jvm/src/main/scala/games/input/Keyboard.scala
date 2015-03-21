package games.input

import org.lwjgl.input.{ Keyboard => LWJGLKeyboard }

object KeyboardLWJGL {
  val mapper = new Keyboard.KeyMapper[Int](
    (Key.Space, LWJGLKeyboard.KEY_SPACE),
    (Key.Apostrophe, LWJGLKeyboard.KEY_APOSTROPHE),
    //(Key.Circumflex, LWJGLKeyboard.KEY_CIRCUMFLEX), // seems buggy
    (Key.Comma, LWJGLKeyboard.KEY_COMMA),
    (Key.Period, LWJGLKeyboard.KEY_PERIOD),
    (Key.Minus, LWJGLKeyboard.KEY_MINUS),
    (Key.Slash, LWJGLKeyboard.KEY_SLASH),
    (Key.N0, LWJGLKeyboard.KEY_0),
    (Key.N1, LWJGLKeyboard.KEY_1),
    (Key.N2, LWJGLKeyboard.KEY_2),
    (Key.N3, LWJGLKeyboard.KEY_3),
    (Key.N4, LWJGLKeyboard.KEY_4),
    (Key.N5, LWJGLKeyboard.KEY_5),
    (Key.N6, LWJGLKeyboard.KEY_6),
    (Key.N7, LWJGLKeyboard.KEY_7),
    (Key.N8, LWJGLKeyboard.KEY_8),
    (Key.N9, LWJGLKeyboard.KEY_9),
    (Key.SemiColon, LWJGLKeyboard.KEY_SEMICOLON),
    (Key.Equal, LWJGLKeyboard.KEY_EQUALS),
    (Key.A, LWJGLKeyboard.KEY_A),
    (Key.B, LWJGLKeyboard.KEY_B),
    (Key.C, LWJGLKeyboard.KEY_C),
    (Key.D, LWJGLKeyboard.KEY_D),
    (Key.E, LWJGLKeyboard.KEY_E),
    (Key.F, LWJGLKeyboard.KEY_F),
    (Key.G, LWJGLKeyboard.KEY_G),
    (Key.H, LWJGLKeyboard.KEY_H),
    (Key.I, LWJGLKeyboard.KEY_I),
    (Key.J, LWJGLKeyboard.KEY_J),
    (Key.K, LWJGLKeyboard.KEY_K),
    (Key.L, LWJGLKeyboard.KEY_L),
    (Key.M, LWJGLKeyboard.KEY_M),
    (Key.N, LWJGLKeyboard.KEY_N),
    (Key.O, LWJGLKeyboard.KEY_O),
    (Key.P, LWJGLKeyboard.KEY_P),
    (Key.Q, LWJGLKeyboard.KEY_Q),
    (Key.R, LWJGLKeyboard.KEY_R),
    (Key.S, LWJGLKeyboard.KEY_S),
    (Key.T, LWJGLKeyboard.KEY_T),
    (Key.U, LWJGLKeyboard.KEY_U),
    (Key.V, LWJGLKeyboard.KEY_V),
    (Key.W, LWJGLKeyboard.KEY_W),
    (Key.X, LWJGLKeyboard.KEY_X),
    (Key.Y, LWJGLKeyboard.KEY_Y),
    (Key.Z, LWJGLKeyboard.KEY_Z),
    (Key.BracketLeft, LWJGLKeyboard.KEY_LBRACKET),
    (Key.BracketRight, LWJGLKeyboard.KEY_RBRACKET),
    (Key.BackSlash, LWJGLKeyboard.KEY_BACKSLASH),
    (Key.GraveAccent, LWJGLKeyboard.KEY_GRAVE),
    (Key.Escape, LWJGLKeyboard.KEY_ESCAPE),
    (Key.Enter, LWJGLKeyboard.KEY_RETURN),
    (Key.Tab, LWJGLKeyboard.KEY_TAB),
    (Key.BackSpace, LWJGLKeyboard.KEY_BACK),
    (Key.Insert, LWJGLKeyboard.KEY_INSERT),
    (Key.Delete, LWJGLKeyboard.KEY_DELETE),
    (Key.Right, LWJGLKeyboard.KEY_RIGHT),
    (Key.Left, LWJGLKeyboard.KEY_LEFT),
    (Key.Down, LWJGLKeyboard.KEY_DOWN),
    (Key.Up, LWJGLKeyboard.KEY_UP),
    (Key.PageUp, LWJGLKeyboard.KEY_PRIOR),
    (Key.PageDown, LWJGLKeyboard.KEY_NEXT),
    (Key.Home, LWJGLKeyboard.KEY_HOME),
    (Key.End, LWJGLKeyboard.KEY_END),
    (Key.CapsLock, LWJGLKeyboard.KEY_CAPITAL),
    (Key.ScrollLock, LWJGLKeyboard.KEY_SCROLL),
    (Key.NumLock, LWJGLKeyboard.KEY_NUMLOCK),
    (Key.PrintScreen, LWJGLKeyboard.KEY_SYSRQ),
    (Key.Pause, LWJGLKeyboard.KEY_PAUSE),
    (Key.F1, LWJGLKeyboard.KEY_F1),
    (Key.F2, LWJGLKeyboard.KEY_F2),
    (Key.F3, LWJGLKeyboard.KEY_F3),
    (Key.F4, LWJGLKeyboard.KEY_F4),
    (Key.F5, LWJGLKeyboard.KEY_F5),
    (Key.F6, LWJGLKeyboard.KEY_F6),
    (Key.F7, LWJGLKeyboard.KEY_F7),
    (Key.F8, LWJGLKeyboard.KEY_F8),
    (Key.F9, LWJGLKeyboard.KEY_F9),
    (Key.F10, LWJGLKeyboard.KEY_F10),
    (Key.F11, LWJGLKeyboard.KEY_F11),
    (Key.F12, LWJGLKeyboard.KEY_F12),
    (Key.F13, LWJGLKeyboard.KEY_F13),
    (Key.F14, LWJGLKeyboard.KEY_F14),
    (Key.F15, LWJGLKeyboard.KEY_F15),
    (Key.F16, LWJGLKeyboard.KEY_F16),
    (Key.F17, LWJGLKeyboard.KEY_F17),
    (Key.F18, LWJGLKeyboard.KEY_F18),
    (Key.F19, LWJGLKeyboard.KEY_F19),
    // Nothing for F20 to F25
    (Key.Num0, LWJGLKeyboard.KEY_NUMPAD0),
    (Key.Num1, LWJGLKeyboard.KEY_NUMPAD1),
    (Key.Num2, LWJGLKeyboard.KEY_NUMPAD2),
    (Key.Num3, LWJGLKeyboard.KEY_NUMPAD3),
    (Key.Num4, LWJGLKeyboard.KEY_NUMPAD4),
    (Key.Num5, LWJGLKeyboard.KEY_NUMPAD5),
    (Key.Num6, LWJGLKeyboard.KEY_NUMPAD6),
    (Key.Num7, LWJGLKeyboard.KEY_NUMPAD7),
    (Key.Num8, LWJGLKeyboard.KEY_NUMPAD8),
    (Key.Num9, LWJGLKeyboard.KEY_NUMPAD9),
    (Key.NumDecimal, LWJGLKeyboard.KEY_DECIMAL),
    (Key.NumDivide, LWJGLKeyboard.KEY_DIVIDE),
    (Key.NumMultiply, LWJGLKeyboard.KEY_MULTIPLY),
    (Key.NumSubstract, LWJGLKeyboard.KEY_SUBTRACT),
    (Key.NumAdd, LWJGLKeyboard.KEY_ADD),
    (Key.NumEnter, LWJGLKeyboard.KEY_NUMPADENTER),
    (Key.NumEqual, LWJGLKeyboard.KEY_NUMPADEQUALS),
    (Key.ShiftLeft, LWJGLKeyboard.KEY_LSHIFT),
    (Key.ShiftRight, LWJGLKeyboard.KEY_RSHIFT),
    (Key.ControlLeft, LWJGLKeyboard.KEY_LCONTROL),
    (Key.ControlRight, LWJGLKeyboard.KEY_RCONTROL),
    (Key.AltLeft, LWJGLKeyboard.KEY_LMENU),
    (Key.AltRight, LWJGLKeyboard.KEY_RMENU),
    (Key.SuperLeft, LWJGLKeyboard.KEY_LMETA),
    (Key.SuperRight, LWJGLKeyboard.KEY_RMETA) //(Key.MenuLeft, 184 ???),
    //(Key.MenuRight, ???),
    )
}

class KeyboardLWJGL() extends Keyboard {
  LWJGLKeyboard.create()

  override def close(): Unit = {
    super.close()
    LWJGLKeyboard.destroy()
  }

  def isKeyDown(key: games.input.Key): Boolean = {
    LWJGLKeyboard.poll()
    KeyboardLWJGL.mapper.getForLocal(key) match {
      case Some(keyCode) => LWJGLKeyboard.isKeyDown(keyCode)
      case None          => false // unsupported key
    }
  }

  def nextEvent(): Option[games.input.KeyboardEvent] = {
    if (LWJGLKeyboard.next()) {
      val keyCode = LWJGLKeyboard.getEventKey
      KeyboardLWJGL.mapper.getForRemote(keyCode) match {
        case Some(key) => {
          val down = LWJGLKeyboard.getEventKeyState()
          Some(KeyboardEvent(key, down))
        }
        case None => nextEvent() // unsupported key, skip to the next event
      }
    } else None
  }
}