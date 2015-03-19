package games.input

import scala.scalajs.js
import org.scalajs.dom

import scala.collection.mutable.Queue
import scala.collection.mutable.Set

import games.JsUtils

import scala.concurrent.Future

object MouseJS {
  val mapper = new Mouse.ButtonMapper[Int](
    (Button.Left, 0),
    (Button.Right, 2),
    (Button.Middle, 1))

  private def getForLocal(button: Button): Int = button match {
    case Button.Aux(num) => num
    case _ => MouseJS.mapper.getForLocal(button) match {
      case Some(num) => num
      case None      => throw new RuntimeException("No known LWJGL code for button " + button)
    }
  }

  private def getForRemote(eventButton: Int): Button = MouseJS.mapper.getForRemote(eventButton) match {
    case Some(button) => button
    case None         => Button.Aux(eventButton)
  }
}

class MouseJS(element: js.Dynamic) extends Mouse {
  def this() = this(dom.document.asInstanceOf[js.Dynamic])
  def this(html: dom.raw.HTMLElement) = this(html.asInstanceOf[js.Dynamic])

  private var mouseInside = false
  private var dx, dy = 0
  private var x, y = 0

  private val eventQueue: Queue[MouseEvent] = Queue()
  private val downButtons: Set[Button] = Set()

  private var lockRequested = false

  private def buttonFromEvent(ev: dom.raw.MouseEvent): Button = {
    val eventButton = ev.button.asInstanceOf[Int]
    val button = MouseJS.getForRemote(eventButton)
    button
  }

  private val onMouseUp: js.Function = (e: dom.raw.MouseEvent) => {
    e.preventDefault()
    JsUtils.flushUserEventTasks()

    val button = buttonFromEvent(e)
    if (this.isButtonDown(button)) {
      downButtons -= button
      eventQueue += ButtonEvent(button, false)
    }
  }
  private val onMouseDown: js.Function = (e: dom.raw.MouseEvent) => {
    e.preventDefault()
    JsUtils.flushUserEventTasks()

    val button = buttonFromEvent(e)
    if (!this.isButtonDown(button)) {
      downButtons += button
      eventQueue += ButtonEvent(button, true)
    }
  }
  private val onMouseMove: js.Function = (e: dom.raw.MouseEvent) => {
    e.preventDefault()
    //JsUtils.flushUserEventTasks() // Apparently, not considered as a user gesture

    val ev = e.asInstanceOf[js.Dynamic]

    // Get relative position
    val movX = JsUtils.getOptional[Int](ev, "movementX", "webkitMovementX", "mozMovementX")
    val movY = JsUtils.getOptional[Int](ev, "movementY", "webkitMovementY", "mozMovementY")

    dx = movX.getOrElse(0)
    dy = movY.getOrElse(0)

    // Get position on element
    val offX = ev.offsetX.asInstanceOf[js.UndefOr[Int]]
    val offY = ev.offsetY.asInstanceOf[js.UndefOr[Int]]

    val (posX, posY) = if (offX.isDefined && offY.isDefined) { // For WebKit browsers
      (offX.get, offY.get)
    } else { // For... the others. From jQuery: https://github.com/jquery/jquery/blob/2.1.3/src/offset.js#L107-L108
      val bounding = element.getBoundingClientRect()
      val window = js.Dynamic.global.window

      val boundingLeft = bounding.left.asInstanceOf[Double]
      val boundingTop = bounding.top.asInstanceOf[Double]

      val winOffsetX = window.pageXOffset.asInstanceOf[Double]
      val winOffsetY = window.pageYOffset.asInstanceOf[Double]

      val elemOffsetX = element.clientLeft.asInstanceOf[Double]
      val elemOffsetY = element.clientTop.asInstanceOf[Double]

      ((e.pageX - (boundingLeft + winOffsetX - elemOffsetX)).toInt, (e.pageY - (boundingTop + winOffsetY - elemOffsetY)).toInt)
    }

    x = posX
    y = posY
  }
  private val onMouseOver: js.Function = (e: dom.raw.MouseEvent) => {
    e.preventDefault()
    //JsUtils.flushUserEventTasks() // Apparently, not considered as a user gesture

    mouseInside = true
  }
  private val onMouseOut: js.Function = (e: dom.raw.MouseEvent) => {
    e.preventDefault()
    //JsUtils.flushUserEventTasks() // Apparently, not considered as a user gesture

    mouseInside = false
  }
  private val onMouseWheel: js.Function = (e: dom.raw.WheelEvent) => {
    e.preventDefault()
    //JsUtils.flushUserEventTasks() // Apparently, not considered as a user gesture

    val ev = e.asInstanceOf[js.Dynamic]

    val wheelX = ev.wheelDeltaX.asInstanceOf[Int]
    val wheelY = ev.wheelDeltaY.asInstanceOf[Int]

    if (wheelY > 0) {
      eventQueue += WheelEvent(Wheel.Up)
    } else if (wheelY < 0) {
      eventQueue += WheelEvent(Wheel.Down)
    } else if (wheelX > 0) {
      eventQueue += WheelEvent(Wheel.Left)
    } else if (wheelX < 0) {
      eventQueue += WheelEvent(Wheel.Right)
    }
  }
  private val onFirefoxMouseWheel: js.Function = (e: dom.raw.WheelEvent) => {
    e.preventDefault()
    //JsUtils.flushUserEventTasks() // Apparently, not considered as a user gesture

    val ev = e.asInstanceOf[js.Dynamic]

    val axis = ev.axis.asInstanceOf[Int]
    val details = ev.detail.asInstanceOf[Int]

    axis match {
      case 2 => eventQueue += WheelEvent(if (details < 0) Wheel.Up else Wheel.Down) // Vertical
      case 1 => eventQueue += WheelEvent(if (details < 0) Wheel.Left else Wheel.Right) // horizontal
      case _ => // unknown
    }
  }

  private val onContextMenu: js.Function = (e: dom.raw.Event) => {
    false // disable right-click context-menu
  }

  private val onPointerLockChange: js.Function = (e: js.Dynamic) => {
    this.locked = lockRequested // If the lock state has changed against the wish of the user, change back ASAP
    //js.Dynamic.global.console.log("onPointerLockChange", this.locked, e)
  }
  private val onPointerLockError: js.Function = (e: js.Dynamic) => {
    // nothing to do?
    js.Dynamic.global.console.log("onPointerLockError", this.locked, e)
  }

  private val document = dom.document.asInstanceOf[js.Dynamic]

  // Init
  {
    element.addEventListener("mouseup", onMouseUp, true)
    element.addEventListener("mousedown", onMouseDown, true)
    element.oncontextmenu = onContextMenu
    element.addEventListener("mousemove", onMouseMove, true)
    element.addEventListener("mouseover", onMouseOver, true)
    element.addEventListener("mouseout", onMouseOut, true)
    element.addEventListener("mousewheel", onMouseWheel, true)
    element.addEventListener("DOMMouseScroll", onFirefoxMouseWheel, true) // Firefox

    document.addEventListener("pointerlockchange", onPointerLockChange, true)
    document.addEventListener("webkitpointerlockchange", onPointerLockChange, true)
    document.addEventListener("mozpointerlockchange", onPointerLockChange, true)

    document.addEventListener("pointerlockerror", onPointerLockError, true)
    document.addEventListener("webkitpointerlockerror", onPointerLockError, true)
    document.addEventListener("mozpointerlockerror", onPointerLockError, true)
  }

  override def close(): Unit = {
    element.removeEventListener("mouseup", onMouseUp, true)
    element.removeEventListener("mousedown", onMouseDown, true)
    element.oncontextmenu = js.undefined
    element.removeEventListener("mousemove", onMouseMove, true)
    element.removeEventListener("mouseover", onMouseOver, true)
    element.removeEventListener("mouseout", onMouseOut, true)
    element.removeEventListener("mousewheel", onMouseWheel, true)
    element.removeEventListener("DOMMouseScroll", onFirefoxMouseWheel, true) // Firefox

    document.removeEventListener("pointerlockchange", onPointerLockChange, true)
    document.removeEventListener("webkitpointerlockchange", onPointerLockChange, true)
    document.removeEventListener("mozpointerlockchange", onPointerLockChange, true)

    document.removeEventListener("pointerlockerror", onPointerLockError, true)
    document.removeEventListener("webkitpointerlockerror", onPointerLockError, true)
    document.removeEventListener("mozpointerlockerror", onPointerLockError, true)
  }

  def position: games.input.Position = {
    Position(x, y)
  }
  def deltaPosition: games.input.Position = {
    val delta = Position(dx, dy)

    // Reset relative position
    dx = 0
    dy = 0

    delta
  }

  val lockRequest = JsUtils.getOptional[js.Dynamic](element, "requestPointerLock", "webkitRequestPointerLock", "mozRequestPointerLock")
  val lockExit = JsUtils.getOptional[js.Dynamic](document, "exitPointerLock", "webkitExitPointerLock", "mozExitPointerLock")

  element.lockRequest = lockRequest.getOrElse(JsUtils.featureUnsupportedFunction("Pointer Lock (Request)"))
  document.lockExit = lockExit.getOrElse(JsUtils.featureUnsupportedFunction("Pointer Lock (Exit)"))

  def locked: Boolean = JsUtils.getOptional[js.Dynamic](document, "pointerLockElement", "webkitPointerLockElement", "mozPointerLockElement") match {
    case Some(el) => el == element
    case None     => false
  }
  def locked_=(locked: Boolean): Unit = {
    lockRequested = locked // Remember the choice of the user

    if (locked && !this.locked) {
      Future {
        element.lockRequest()
      }(JsUtils.userEventExecutionContext)
    } else if (!locked && this.locked) {
      Future {
        document.lockExit()
      }(JsUtils.userEventExecutionContext)
    }
  }

  def isButtonDown(button: games.input.Button): Boolean = {
    downButtons.contains(button)
  }
  def nextEvent(): Option[games.input.MouseEvent] = {
    if (eventQueue.nonEmpty) Some(eventQueue.dequeue())
    else None
  }

  def isInside(): Boolean = mouseInside
}