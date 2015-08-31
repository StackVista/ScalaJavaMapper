package com.stackstate.scalajavamapper

trait Converters {
  implicit def identityConverter[T <: Any] = new Converter[T, T] {
    def write(s: T) = s
    def read(j: T) = j
  }

  private def baseNumberTypesConverter[T <: AnyVal, J] = new Converter[T, J] {
    def write(s: T) = s.asInstanceOf[J]
    def read(j: J) = j.asInstanceOf[T]
  }

  implicit val byteConverter = baseNumberTypesConverter[Byte, java.lang.Byte]
  implicit val integerConverter = baseNumberTypesConverter[Int, java.lang.Integer]
  implicit val longConverter = baseNumberTypesConverter[Long, java.lang.Long]
  implicit val floatConverter = baseNumberTypesConverter[Float, java.lang.Float]
  implicit val doubleConverter = baseNumberTypesConverter[Double, java.lang.Double]
  implicit val charConverter = baseNumberTypesConverter[Char, java.lang.Character]

  implicit val booleanTypesConverter = new Converter[Boolean, java.lang.Boolean] {
    def write(s: Boolean) = s.asInstanceOf[java.lang.Boolean]
    def read(j: java.lang.Boolean) = j.asInstanceOf[Boolean]
  }

  implicit def seqToListReader[T, J](implicit reader: JavaReader[T, J]) = new JavaReader[Seq[T], java.util.List[J]] {
    import scala.collection.JavaConversions

    def read(list: java.util.List[J]): Seq[T] = {
      if (list == null) null
      else JavaConversions.asScalaIterator(list.iterator()).map(Mapper.fromJava[T, J]).toVector
    }
  }

  implicit def seqToListWriter[T, J](implicit writer: JavaWriter[T, J]) = new JavaWriter[Seq[T], java.util.List[J]] {
    import scala.collection.JavaConversions

    // Make sure that a copy is made of the lists instead of wrapping the existing list
    def write(list: Seq[T]): java.util.List[J] = {
      if (list == null) null
      else new java.util.ArrayList(JavaConversions.seqAsJavaList(list.map(Mapper.toJava[T, J])))
    }
  }

  implicit def setToSetReader[T, J](implicit reader: JavaReader[T, J]) = new JavaReader[Set[T], java.util.Set[J]] {
    import scala.collection.JavaConversions

    def read(set: java.util.Set[J]): Set[T] = {
      if (set == null) null
      else JavaConversions.asScalaIterator(set.iterator()).map(Mapper.fromJava[T, J]).toSet
    }
  }

  implicit def setToSetWriter[T, J](implicit writer: JavaWriter[T, J]) = new JavaWriter[Set[T], java.util.Set[J]] {
    import scala.collection.JavaConversions

    def write(set: Set[T]): java.util.Set[J] = {
      if (set == null) null
      else new java.util.HashSet(JavaConversions.setAsJavaSet(set.map(Mapper.toJava[T, J])))
    }
  }

  implicit def optionToTypeReader[T, J](implicit reader: JavaReader[T, J]) = new JavaReader[Option[T], J] {
    def read(value: J): Option[T] = Option(value).map(Mapper.fromJava[T, J])
  }

  implicit def optionToTypeWriter[T, J](implicit converter: JavaWriter[T, J]) = new JavaWriter[Option[T], J] {
    def write(option: Option[T]): J = option.map(Mapper.toJava[T, J]).getOrElse(null.asInstanceOf[J])
  }
}

object Converters extends Converters
