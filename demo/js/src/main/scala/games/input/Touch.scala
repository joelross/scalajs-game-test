package games.input

import scala.scalajs.js
import org.scalajs.dom

import scala.collection.mutable
import scala.collection.immutable

import games.JsUtils

class TouchpadJS(element: js.Dynamic) extends Touchpad {
  def this() = this(dom.document.asInstanceOf[js.Dynamic])
  def this(any: js.Any) = this(any.asInstanceOf[js.Dynamic])

  private val eventQueue: mutable.Queue[TouchEvent] = mutable.Queue()

  private val onTouchStart: js.Function = (e: dom.raw.TouchEvent) => {
    if (lockRequested) e.preventDefault()
    JsUtils.flushUserEventTasks()

    val list = e.changedTouches
    for (i <- 0 until list.length) {
      val touch = list(i)
      val identifier = touch.identifier
      // Is there an offsetX/offsetY for such element?
      val (offsetX, offsetY) = JsUtils.offsetOfElement(element)
      val pos = Position((touch.pageX - offsetX).toInt, (touch.pageY - offsetY).toInt)
      val data = Touch(identifier, pos)
      eventQueue += TouchStart(data)
    }
  }
  private val onTouchEnd: js.Function = (e: dom.raw.TouchEvent) => {
    if (lockRequested) e.preventDefault()
    JsUtils.flushUserEventTasks()

    val list = e.changedTouches
    for (i <- 0 until list.length) {
      val touch = list(i)
      val identifier = touch.identifier
      // Is there an offsetX/offsetY for such element?
      val (offsetX, offsetY) = JsUtils.offsetOfElement(element)
      val pos = Position((touch.pageX - offsetX).toInt, (touch.pageY - offsetY).toInt)
      val data = Touch(identifier, pos)
      eventQueue += TouchEnd(data)
    }
  }
  private val onTouchMove: js.Function = (e: dom.raw.TouchEvent) => {
    if (lockRequested) e.preventDefault()
    JsUtils.flushUserEventTasks()
  }
  private val onTouchCancel: js.Function = (e: dom.raw.TouchEvent) => {
    if (lockRequested) e.preventDefault()
    JsUtils.flushUserEventTasks()
  }

  // Init
  {
    element.addEventListener("touchstart", onTouchStart, true)
    element.addEventListener("touchend", onTouchEnd, true)
    element.addEventListener("touchleave", onTouchEnd, true)
    element.addEventListener("touchcancel", onTouchCancel, true)
    element.addEventListener("touchmove", onTouchMove, true)
  }

  override def close(): Unit = {
    element.removeEventListener("touchstart", onTouchStart, true)
    element.removeEventListener("touchend", onTouchEnd, true)
    element.removeEventListener("touchleave", onTouchEnd, true)
    element.removeEventListener("touchcancel", onTouchCancel, true)
    element.removeEventListener("touchmove", onTouchMove, true)
  }

  def nextEvent(): Option[games.input.TouchEvent] = {
    if (eventQueue.nonEmpty) Some(eventQueue.dequeue())
    else None
  }
  def touches: Seq[games.input.Touch] = ???

  private var lockRequested: Boolean = false

  def locked: Boolean = lockRequested
  def locked_=(locked: Boolean): Unit = {
    lockRequested = locked
  }
}