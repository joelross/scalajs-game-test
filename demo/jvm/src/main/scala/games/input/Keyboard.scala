package games.input

import org.lwjgl.glfw._
import org.lwjgl.system.MemoryUtil.{ NULL => LWJGL_NULL }

import scala.concurrent.{ Future }

object KeyboardLWJGL {
  val mapper = new Keyboard.KeyMapper[Int](
    (Key.Space, GLFW.GLFW_KEY_SPACE),
    (Key.Apostrophe, GLFW.GLFW_KEY_APOSTROPHE),
    //(Key.Circumflex, LWJGLKeyboard.KEY_CIRCUMFLEX), // seems buggy
    (Key.Comma, GLFW.GLFW_KEY_COMMA),
    (Key.Period, GLFW.GLFW_KEY_PERIOD),
    (Key.Minus, GLFW.GLFW_KEY_MINUS),
    (Key.Slash, GLFW.GLFW_KEY_SLASH),
    (Key.N0, GLFW.GLFW_KEY_0),
    (Key.N1, GLFW.GLFW_KEY_1),
    (Key.N2, GLFW.GLFW_KEY_2),
    (Key.N3, GLFW.GLFW_KEY_3),
    (Key.N4, GLFW.GLFW_KEY_4),
    (Key.N5, GLFW.GLFW_KEY_5),
    (Key.N6, GLFW.GLFW_KEY_6),
    (Key.N7, GLFW.GLFW_KEY_7),
    (Key.N8, GLFW.GLFW_KEY_8),
    (Key.N9, GLFW.GLFW_KEY_9),
    (Key.SemiColon, GLFW.GLFW_KEY_SEMICOLON),
    (Key.Equal, GLFW.GLFW_KEY_EQUAL),
    (Key.A, GLFW.GLFW_KEY_A),
    (Key.B, GLFW.GLFW_KEY_B),
    (Key.C, GLFW.GLFW_KEY_C),
    (Key.D, GLFW.GLFW_KEY_D),
    (Key.E, GLFW.GLFW_KEY_E),
    (Key.F, GLFW.GLFW_KEY_F),
    (Key.G, GLFW.GLFW_KEY_G),
    (Key.H, GLFW.GLFW_KEY_H),
    (Key.I, GLFW.GLFW_KEY_I),
    (Key.J, GLFW.GLFW_KEY_J),
    (Key.K, GLFW.GLFW_KEY_K),
    (Key.L, GLFW.GLFW_KEY_L),
    (Key.M, GLFW.GLFW_KEY_M),
    (Key.N, GLFW.GLFW_KEY_N),
    (Key.O, GLFW.GLFW_KEY_O),
    (Key.P, GLFW.GLFW_KEY_P),
    (Key.Q, GLFW.GLFW_KEY_Q),
    (Key.R, GLFW.GLFW_KEY_R),
    (Key.S, GLFW.GLFW_KEY_S),
    (Key.T, GLFW.GLFW_KEY_T),
    (Key.U, GLFW.GLFW_KEY_U),
    (Key.V, GLFW.GLFW_KEY_V),
    (Key.W, GLFW.GLFW_KEY_W),
    (Key.X, GLFW.GLFW_KEY_X),
    (Key.Y, GLFW.GLFW_KEY_Y),
    (Key.Z, GLFW.GLFW_KEY_Z),
    (Key.BracketLeft, GLFW.GLFW_KEY_LEFT_BRACKET),
    (Key.BracketRight, GLFW.GLFW_KEY_RIGHT_BRACKET),
    (Key.BackSlash, GLFW.GLFW_KEY_BACKSLASH),
    (Key.GraveAccent, GLFW.GLFW_KEY_GRAVE_ACCENT),
    (Key.Escape, GLFW.GLFW_KEY_ESCAPE),
    (Key.Enter, GLFW.GLFW_KEY_ENTER),
    (Key.Tab, GLFW.GLFW_KEY_TAB),
    (Key.BackSpace, GLFW.GLFW_KEY_BACKSPACE),
    (Key.Insert, GLFW.GLFW_KEY_INSERT),
    (Key.Delete, GLFW.GLFW_KEY_DELETE),
    (Key.Right, GLFW.GLFW_KEY_RIGHT),
    (Key.Left, GLFW.GLFW_KEY_LEFT),
    (Key.Down, GLFW.GLFW_KEY_DOWN),
    (Key.Up, GLFW.GLFW_KEY_UP),
    (Key.PageUp, GLFW.GLFW_KEY_PAGE_UP),
    (Key.PageDown, GLFW.GLFW_KEY_PAGE_DOWN),
    (Key.Home, GLFW.GLFW_KEY_HOME),
    (Key.End, GLFW.GLFW_KEY_END),
    (Key.CapsLock, GLFW.GLFW_KEY_CAPS_LOCK),
    (Key.ScrollLock, GLFW.GLFW_KEY_SCROLL_LOCK),
    (Key.NumLock, GLFW.GLFW_KEY_NUM_LOCK),
    (Key.PrintScreen, GLFW.GLFW_KEY_PRINT_SCREEN),
    (Key.Pause, GLFW.GLFW_KEY_PAUSE),
    (Key.F1, GLFW.GLFW_KEY_F1),
    (Key.F2, GLFW.GLFW_KEY_F2),
    (Key.F3, GLFW.GLFW_KEY_F3),
    (Key.F4, GLFW.GLFW_KEY_F4),
    (Key.F5, GLFW.GLFW_KEY_F5),
    (Key.F6, GLFW.GLFW_KEY_F6),
    (Key.F7, GLFW.GLFW_KEY_F7),
    (Key.F8, GLFW.GLFW_KEY_F8),
    (Key.F9, GLFW.GLFW_KEY_F9),
    (Key.F10, GLFW.GLFW_KEY_F10),
    (Key.F11, GLFW.GLFW_KEY_F11),
    (Key.F12, GLFW.GLFW_KEY_F12),
    (Key.F13, GLFW.GLFW_KEY_F13),
    (Key.F14, GLFW.GLFW_KEY_F14),
    (Key.F15, GLFW.GLFW_KEY_F15),
    (Key.F16, GLFW.GLFW_KEY_F16),
    (Key.F17, GLFW.GLFW_KEY_F17),
    (Key.F18, GLFW.GLFW_KEY_F18),
    (Key.F19, GLFW.GLFW_KEY_F19),
    (Key.F20, GLFW.GLFW_KEY_F20),
    (Key.F21, GLFW.GLFW_KEY_F21),
    (Key.F22, GLFW.GLFW_KEY_F22),
    (Key.F23, GLFW.GLFW_KEY_F23),
    (Key.F24, GLFW.GLFW_KEY_F24),
    (Key.F25, GLFW.GLFW_KEY_F25),
    (Key.Num0, GLFW.GLFW_KEY_KP_0),
    (Key.Num1, GLFW.GLFW_KEY_KP_1),
    (Key.Num2, GLFW.GLFW_KEY_KP_2),
    (Key.Num3, GLFW.GLFW_KEY_KP_3),
    (Key.Num4, GLFW.GLFW_KEY_KP_4),
    (Key.Num5, GLFW.GLFW_KEY_KP_5),
    (Key.Num6, GLFW.GLFW_KEY_KP_6),
    (Key.Num7, GLFW.GLFW_KEY_KP_7),
    (Key.Num8, GLFW.GLFW_KEY_KP_8),
    (Key.Num9, GLFW.GLFW_KEY_KP_9),
    (Key.NumDecimal, GLFW.GLFW_KEY_KP_DECIMAL),
    (Key.NumDivide, GLFW.GLFW_KEY_KP_DIVIDE),
    (Key.NumMultiply, GLFW.GLFW_KEY_KP_MULTIPLY),
    (Key.NumSubstract, GLFW.GLFW_KEY_KP_SUBTRACT),
    (Key.NumAdd, GLFW.GLFW_KEY_KP_ADD),
    (Key.NumEnter, GLFW.GLFW_KEY_KP_ENTER),
    (Key.NumEqual, GLFW.GLFW_KEY_KP_EQUAL),
    (Key.ShiftLeft, GLFW.GLFW_KEY_LEFT_SHIFT),
    (Key.ShiftRight, GLFW.GLFW_KEY_RIGHT_SHIFT),
    (Key.ControlLeft, GLFW.GLFW_KEY_LEFT_CONTROL),
    (Key.ControlRight, GLFW.GLFW_KEY_RIGHT_CONTROL),
    (Key.AltLeft, GLFW.GLFW_KEY_LEFT_ALT),
    (Key.AltRight, GLFW.GLFW_KEY_RIGHT_ALT),
    (Key.SuperLeft, GLFW.GLFW_KEY_LEFT_SUPER),
    (Key.SuperRight, GLFW.GLFW_KEY_RIGHT_SUPER)
    //(Key.MenuLeft, 184 ???),
    //(Key.MenuRight, ???),
    )
}

class KeyboardLWJGL(display: games.opengl.Display) extends Keyboard {
  private val window: games.opengl.GLFWWindow = display.asInstanceOf[games.opengl.GLFWWindow]
  
  private val keyCallback: GLFWKeyCallback = new GLFWKeyCallback() {
    def invoke(window: Long, key: Int, scanCode: Int, action: Int, mods: Int): Unit = {
      println("### Key callback")
      // TODO
    }
  }
  
  { // Init
    games.JvmUtils.await(Future {
      GLFW.glfwSetKeyCallback(window.pointer, keyCallback)
    } (games.JvmUtils.getGLFWManager().mainExecutionContext))("Could not register key callback")
  }

  override def close(): Unit = {
    super.close()
    
    games.JvmUtils.await(Future {
      GLFW.glfwSetKeyCallback(window.pointer, null) // TODO ok, is this really null, LWJGL_NULL, or something else ?
      keyCallback.free()
    } (games.JvmUtils.getGLFWManager().mainExecutionContext))("Could not register key callback")
  }

  def isKeyDown(key: games.input.Key): Boolean = {
    ??? //LWJGLKeyboard.poll()
    KeyboardLWJGL.mapper.getForLocal(key) match {
      case Some(keyCode) => ??? //LWJGLKeyboard.isKeyDown(keyCode)
      case None          => false // unsupported key
    }
  }

  def nextEvent(): Option[games.input.KeyboardEvent] = {
    if (??? /*LWJGLKeyboard.next()*/) {
      val keyCode = ??? //LWJGLKeyboard.getEventKey
      KeyboardLWJGL.mapper.getForRemote(keyCode) match {
        case Some(key) =>
          val down = ??? //LWJGLKeyboard.getEventKeyState()
          Some(KeyboardEvent(key, down))

        case None => nextEvent() // unsupported key, skip to the next event
      }
    } else None
  }
}
