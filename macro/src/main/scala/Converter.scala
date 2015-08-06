package com.stackstate.objectmapper.macros

import java.util

import scala.annotation.implicitNotFound
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

trait ToJava[T, J] {
  def toJava(t: T): J
}

trait FromJava[T, J] {
  def fromJava(j: J): T
}

@implicitNotFound("Required Converter from scala type ${T} to java type ${J} not found.")
trait Converter[T, J] extends ToJava[T, J] with FromJava[T, J]

trait LowerImplicits {

}

object Converter extends LowerImplicits {
  implicit def baseTypesConverter[T <: Any] = new Converter[T, T] {
    def toJava(s: T) = s
    def fromJava(j: T) = j
  }

  implicit def baseNumberTypesConverter[T <: AnyVal, J <: Number] = new Converter[T, J] {
    def toJava(s: T) = s.asInstanceOf[J]
    def fromJava(j: J) = j.asInstanceOf[T]
  }

  implicit def seqToListConverter[T, J](implicit converter: Converter[T, J]) = new Converter[Seq[T], java.util.List[J]] {
    import scala.collection.JavaConversions

    // Make sure that a copy is made of the lists instead of wrapping the existing list
    def toJava(list: Seq[T]): java.util.List[J] = new java.util.ArrayList(JavaConversions.seqAsJavaList(list.map(Converter.toJava[T, J])))

    def fromJava(list: java.util.List[J]): Seq[T] = {
      JavaConversions.asScalaIterator(list.iterator()).map(Converter.fromJava[T, J]).toVector
    }
  }

  implicit def setToSetConverter[T, J](implicit converter: Converter[T, J]) = new Converter[Set[T], java.util.Set[J]] {
    import scala.collection.JavaConversions

    def toJava(set: Set[T]): java.util.Set[J] = new java.util.HashSet(JavaConversions.setAsJavaSet(set.map(Converter.toJava[T, J])))
    def fromJava(set: java.util.Set[J]): Set[T] = JavaConversions.asScalaIterator(set.iterator()).map(Converter.fromJava[T, J]).toSet
  }

  implicit def optionToTypeConverter[T, J](implicit converter: Converter[T, J]) = new Converter[Option[T], J] {
    def toJava(option: Option[T]): J = option.map(Converter.toJava[T, J]).getOrElse(null.asInstanceOf[J])
    def fromJava(value: J): Option[T] = Option(value).map(Converter.fromJava[T, J])
  }

  def toJava[T, J](t: T)(implicit converter: Converter[T, J]): J = converter.toJava(t)
  def fromJava[T, J](j: J)(implicit converter: Converter[T, J]): T = converter.fromJava(j)

  def converter[T, J]: Converter[T, J] = macro converterMacro[T, J]

  def converterMacro[T: c.WeakTypeTag, J: c.WeakTypeTag](c: Context): c.Expr[Converter[T, J]] = {
    import c.universe._
    val tpe = weakTypeOf[T]
    val tpj = weakTypeOf[J]

    val companion = tpe.typeSymbol.companion
    val fields = tpe.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor ⇒ m
    }.get.paramLists.head

    val (toJavaStatements, fromJavaParams) = fields.map { field ⇒
      val name = field.asTerm.name
      val decoded = name.decodedName.toString
      val scalaFieldType = tpe.decl(name).typeSignature

      val setterName = s"set${decoded.capitalize}"
      val javaSetter = tpj.members.find(_.asTerm.name.decodedName.toString == setterName).getOrElse(
        c.abort(c.enclosingPosition, s"Setter $setterName not found on ${tpj.typeSymbol.name}.")
      )
      val javaSetterName = javaSetter.asTerm.name
      val javaSetterParams = javaSetter.typeSignature.paramLists.head
      if (javaSetterParams.size != 1) {
        c.abort(c.enclosingPosition, s"Setter ${javaSetterName} requires exactly one parameter, found ${javaSetterParams.size}")
      }
      val javaSetterType = javaSetterParams.head.typeSignature

      val getterName = s"get${decoded.capitalize}"
      val javaGetter = tpj.members.find(_.asTerm.name.decodedName.toString == getterName).getOrElse(
        c.abort(c.enclosingPosition, s"Getter $getterName not found on ${tpj.typeSymbol.name}.")
      )
      val javaGetterName = javaGetter.asTerm.name
      val javaGetterType = javaGetter.typeSignature.resultType

      (q"""javaObj.$javaSetterName(com.stackstate.objectmapper.macros.Converter.toJava[$scalaFieldType, $javaSetterType](t.$name))""",
        q"""com.stackstate.objectmapper.macros.Converter.fromJava[$scalaFieldType, $javaGetterType](j.$javaGetterName)""")
    }.unzip

    val setJavaFields = toJavaStatements.foldLeft(q"")((acc, next) => q"..$acc; $next")

    c.Expr[Converter[T, J]] {
      q"""
  new Converter[$tpe, $tpj] {
    def toJava(t: $tpe): $tpj = {
      val javaObj = new $tpj()
      $setJavaFields
      javaObj
    }

    def fromJava(j: $tpj): $tpe = $companion(..$fromJavaParams)
  }
  """
    }
  }
}
