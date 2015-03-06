package games.math

abstract sealed class MajorOrder

case object RowMajor extends MajorOrder {
  override def toString = "Row-major"
}

case object ColumnMajor extends MajorOrder {
  override def toString = "Column-major"
}
