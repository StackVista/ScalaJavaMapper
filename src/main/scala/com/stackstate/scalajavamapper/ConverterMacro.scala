package com.stackstate.scalajavamapper

import scala.reflect.macros.whitebox.Context

object ConverterMacro {
  def converter[T: c.WeakTypeTag, J: c.WeakTypeTag](c: Context): c.Expr[Converter[T, J]] = {
    import c.universe._
    val tpeCaseClass = weakTypeOf[T]
    val tpeJavaClass = weakTypeOf[J]

    def javaSetter(fieldName: String): (c.universe.TermName, c.universe.Type) = {
      val setterName = s"set${fieldName.capitalize}"
      val javaSetter = tpeJavaClass.members.find(_.asTerm.name.decodedName.toString == setterName).getOrElse(
        c.abort(c.enclosingPosition, s"Setter $setterName not found on ${tpeJavaClass.typeSymbol.name}.")
      )
      val javaSetterName = javaSetter.asTerm.name
      val javaSetterParams = javaSetter.typeSignature.paramLists.head
      if (javaSetterParams.size != 1) {
        c.abort(c.enclosingPosition, s"Setter ${javaSetterName} requires exactly one parameter, found ${javaSetterParams.size}")
      }
      val javaSetterType = javaSetterParams.head.typeSignature
      (javaSetterName, javaSetterType)
    }

    def javaGetter(fieldName: String): (c.universe.TermName, c.universe.Type) = {
      val getterName = s"get${fieldName.capitalize}"
      val javaGetter = tpeJavaClass.members.find(_.asTerm.name.decodedName.toString == getterName).getOrElse(
        c.abort(c.enclosingPosition, s"Getter $getterName not found on ${tpeJavaClass.typeSymbol.name}.")
      )
      val javaGetterName = javaGetter.asTerm.name
      val javaGetterType = javaGetter.typeSignature.resultType
      (javaGetterName, javaGetterType)
    }

    val companion = tpeCaseClass.typeSymbol.companion
    val fields = tpeCaseClass.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor ⇒ m
    }.get.paramLists.head

    val (toJavaStatements, fromJavaParams) = fields.map { field ⇒
      val scalaFieldName = field.asTerm.name
      val decodedFieldName = scalaFieldName.decodedName.toString
      val scalaFieldType = tpeCaseClass.decl(scalaFieldName).typeSignature

      val (javaSetterName, javaSetterType) = javaSetter(decodedFieldName)
      val (javaGetterName, javaGetterType) = javaGetter(decodedFieldName)

      (q"""javaObj.$javaSetterName(com.stackstate.scalajavamapper.Converter.toJava[$scalaFieldType, $javaSetterType](t.$scalaFieldName))""",
        q"""com.stackstate.scalajavamapper.Converter.fromJava[$scalaFieldType, $javaGetterType](j.$javaGetterName)""")
    }.unzip

    val setJavaFields = toJavaStatements.foldLeft(q"")((acc, next) => q"..$acc; $next")

    c.Expr[Converter[T, J]] {
      q"""
  new Converter[$tpeCaseClass, $tpeJavaClass] {
    def write(t: $tpeCaseClass): $tpeJavaClass = {
      val javaObj = new $tpeJavaClass()
      $setJavaFields
      javaObj
    }

    def read(j: $tpeJavaClass): $tpeCaseClass = $companion(..$fromJavaParams)
  }
  """
    }
  }
}
