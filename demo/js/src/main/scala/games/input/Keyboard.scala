package games.input

class KeyboardJS extends Keyboard {
  def isKeyDown(key: games.input.Key): Boolean = ???
  def nextEvent(): Option[games.input.KeyboardEvent] = ???
}