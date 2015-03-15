package games.input

private[games] class BiMap[R, T](entries: (R, T)*) {
  private val map = entries.toMap
  private val reverseMap = entries.map { case (a, b) => (b, a) }.toMap

  def getForLocal(loc: R): Option[T] = map.get(loc)
  def getForRemote(rem: T): Option[R] = reverseMap.get(rem)
}