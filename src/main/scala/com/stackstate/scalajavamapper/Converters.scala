package com.stackstate.scalajavamapper

trait Converters {
  implicit def baseTypesConverter[T <: Any] = new Converter[T, T] {
    def write(s: T) = s
    def read(j: T) = j
  }

  implicit def baseNumberTypesConverter[T <: AnyVal, J <: Number] = new Converter[T, J] {
    def write(s: T) = s.asInstanceOf[J]
    def read(j: J) = j.asInstanceOf[T]
  }

  implicit val booleanTypesConverter = new Converter[Boolean, java.lang.Boolean] {
    def write(s: Boolean) = s.asInstanceOf[java.lang.Boolean]
    def read(j: java.lang.Boolean) = j.asInstanceOf[Boolean]
  }

  implicit def seqToListConverter[T, J](implicit converter: Converter[T, J]) = new Converter[Seq[T], java.util.List[J]] {
    import scala.collection.JavaConversions

    // Make sure that a copy is made of the lists instead of wrapping the existing list
    def write(list: Seq[T]): java.util.List[J] = new java.util.ArrayList(JavaConversions.seqAsJavaList(list.map(Converter.toJava[T, J])))

    def read(list: java.util.List[J]): Seq[T] = {
      JavaConversions.asScalaIterator(list.iterator()).map(Converter.fromJava[T, J]).toVector
    }
  }

  implicit def setToSetConverter[T, J](implicit converter: Converter[T, J]) = new Converter[Set[T], java.util.Set[J]] {
    import scala.collection.JavaConversions

    def write(set: Set[T]): java.util.Set[J] = new java.util.HashSet(JavaConversions.setAsJavaSet(set.map(Converter.toJava[T, J])))
    def read(set: java.util.Set[J]): Set[T] = JavaConversions.asScalaIterator(set.iterator()).map(Converter.fromJava[T, J]).toSet
  }

  implicit def optionToTypeConverter[T, J](implicit converter: Converter[T, J]) = new Converter[Option[T], J] {
    def write(option: Option[T]): J = option.map(Converter.toJava[T, J]).getOrElse(null.asInstanceOf[J])
    def read(value: J): Option[T] = Option(value).map(Converter.fromJava[T, J])
  }
}

object Converters extends Converters
