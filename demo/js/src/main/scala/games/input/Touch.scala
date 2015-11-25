package games.input

import scala.scalajs.js
import org.scalajs.dom

import scala.collection.mutable
import scala.collection.immutable

import games.JsUtils

class TouchscreenJS(element: js.Dynamic) extends Touchscreen {
  def this(el: dom.html.Element) = this(el.asInstanceOf[js.Dynamic])
  def this(doc: dom.html.Document) = this(doc.asInstanceOf[js.Dynamic])

  private val eventQueue: mutable.Queue[TouchEvent] = mutable.Queue()
  private val touchsMap: mutable.Map[Int, Touch] = mutable.Map()

  private var nextId: Int = 0

  private val onTouchStart: js.Function = (e: dom.TouchEvent) => {
    if (preventMouse) e.preventDefault()
    JsUtils.flushUserEventTasks()

    val list = e.changedTouches
    for (i <- 0 until list.length) {
      val touchJs = list(i)
      val prvId = touchJs.identifier.toInt // TODO check identifier is Double?
      val pubId = nextId
      nextId += 1

      // Is there an offsetX/offsetY for such element?
      val (offsetX, offsetY) = JsUtils.offsetOfElement(element)
      val pos = Position((touchJs.pageX - offsetX).toInt, (touchJs.pageY - offsetY).toInt)
      val data = Touch(pubId, pos)
      touchsMap += (prvId -> data)
      eventQueue += TouchEvent(data, true)
    }
  }
  private val onTouchEnd: js.Function = (e: dom.TouchEvent) => {
    if (preventMouse) e.preventDefault()
    JsUtils.flushUserEventTasks()

    val list = e.changedTouches
    for (i <- 0 until list.length) {
      val touchJs = list(i)
      val prvId = touchJs.identifier.toInt // TODO check identifier is Double?
      val pubId = touchsMap(prvId).identifier

      // Is there an offsetX/offsetY for such element?
      val (offsetX, offsetY) = JsUtils.offsetOfElement(element)
      val pos = Position((touchJs.pageX - offsetX).toInt, (touchJs.pageY - offsetY).toInt)
      val data = Touch(pubId, pos)
      touchsMap -= prvId
      eventQueue += TouchEvent(data, false)
    }
  }
  private val onTouchMove: js.Function = (e: dom.TouchEvent) => {
    if (preventMouse) e.preventDefault()
    JsUtils.flushUserEventTasks()

    val list = e.changedTouches
    for (i <- 0 until list.length) {
      val touchJs = list(i)
      val prvId = touchJs.identifier.toInt // TODO check identifier is Double?
      val pubId = touchsMap(prvId).identifier

      // Is there an offsetX/offsetY for such element?
      val (offsetX, offsetY) = JsUtils.offsetOfElement(element)
      val pos = Position((touchJs.pageX - offsetX).toInt, (touchJs.pageY - offsetY).toInt)
      val data = Touch(pubId, pos)
      touchsMap += (prvId -> data)
    }
  }

  // Init
  {
    element.addEventListener("touchstart", onTouchStart, true)
    element.addEventListener("touchend", onTouchEnd, true)
    element.addEventListener("touchleave", onTouchEnd, true)
    element.addEventListener("touchcancel", onTouchEnd, true)
    element.addEventListener("touchmove", onTouchMove, true)
  }

  override def close(): Unit = {
    element.removeEventListener("touchstart", onTouchStart, true)
    element.removeEventListener("touchend", onTouchEnd, true)
    element.removeEventListener("touchleave", onTouchEnd, true)
    element.removeEventListener("touchcancel", onTouchEnd, true)
    element.removeEventListener("touchmove", onTouchMove, true)
  }

  def nextEvent(): Option[games.input.TouchEvent] = {
    if (eventQueue.nonEmpty) Some(eventQueue.dequeue())
    else None
  }
  def touches: Seq[games.input.Touch] = {
    touchsMap.values.toSeq
  }

  private var preventMouse: Boolean = true
}
