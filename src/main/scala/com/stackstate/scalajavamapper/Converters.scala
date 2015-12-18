package com.stackstate.scalajavamapper
import scala.collection.JavaConversions

trait AutoConverters extends Converters with AutoMacroConverters { self : BaseMapper =>
}

trait Converters { self : BaseMapper =>
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

  implicit def seqToListReader[T, J](implicit reader: JavaReader[T, J]) =
    JavaReader[Seq[T], java.util.List[J]] { (list: java.util.List[J]) =>
      if (list == null) null
      else JavaConversions.asScalaIterator(list.iterator()).map(fromJava[T, J]).toVector
    }

  implicit def seqToListWriter[T, J](implicit writer: JavaWriter[T, J]) =
    JavaWriter[Seq[T], java.util.List[J]] { (list: Seq[T]) =>
      if (list == null) null
      else new java.util.ArrayList(JavaConversions.seqAsJavaList(list.map(toJava[T, J])))
    }

  implicit def setToSetReader[T, J](implicit reader: JavaReader[T, J]) =
    JavaReader[Set[T], java.util.Set[J]] { (set: java.util.Set[J]) =>
      if (set == null) null
      else JavaConversions.asScalaIterator(set.iterator()).map(fromJava[T, J]).toSet
   }

  implicit def setToSetWriter[T, J](implicit writer: JavaWriter[T, J]) = JavaWriter[Set[T], java.util.Set[J]] { (set: Set[T]) =>
    if (set == null) null
    else new java.util.HashSet(JavaConversions.setAsJavaSet(set.map(toJava[T, J])))
  }

  implicit def optionToTypeReader[T, J](implicit reader: JavaReader[T, J]) =
    JavaReader[Option[T], J](Option(_: J).map(fromJava[T, J]))

  implicit def optionToTypeWriter[T, J](implicit converter: JavaWriter[T, J]) =
    JavaWriter[Option[T], J]((_: Option[T]).map(toJava[T, J]).getOrElse(null.asInstanceOf[J]))

  implicit def optionToOptionalReader[T, J](implicit reader: JavaReader[T, J]) =
    JavaReader[Option[T], java.util.Optional[J]]{ (value: java.util.Optional[J]) =>
      Option(value.orElse(null.asInstanceOf[J])).map(fromJava[T, J])
    }

  implicit def optionToOptionalWriter[T, J](implicit converter: JavaWriter[T, J]) =
    JavaWriter[Option[T], java.util.Optional[J]] { (option: Option[T]) =>
      java.util.Optional.ofNullable(option.map(toJava[T, J]).getOrElse(null.asInstanceOf[J]))
    }
}
