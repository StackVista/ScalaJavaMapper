package com.stackstate.objectmapper.macros

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

object Converter {

  implicit def baseTypesConverter[AnyVal] = new Converter[AnyVal, AnyVal] {
    def toJava(s: AnyVal) = s
    def fromJava(j: AnyVal) = j
  }

  implicit def seqToListConverter[T] = new Converter[Seq[T], java.util.List[T]] {
    def toJava(list: Seq[T]): java.util.List[T] = {
      ???
    }
    def fromJava(list: java.util.List[T]): Seq[T] = {
      ???
    }
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

      //TODO: Validate types and throw an error when option is None
      //TODO: Use Converter to convert types in this type
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
