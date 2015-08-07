package com.stackstate.scalajavamapper

import scala.reflect.macros.whitebox.Context

object ConverterMacro {
  def converter[T: c.WeakTypeTag, J: c.WeakTypeTag](c: Context): c.Expr[Converter[T, J]] = {
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

      (q"""javaObj.$javaSetterName(com.stackstate.scalajavamapper.Converter.toJava[$scalaFieldType, $javaSetterType](t.$name))""",
        q"""com.stackstate.scalajavamapper.Converter.fromJava[$scalaFieldType, $javaGetterType](j.$javaGetterName)""")
    }.unzip

    val setJavaFields = toJavaStatements.foldLeft(q"")((acc, next) => q"..$acc; $next")

    c.Expr[Converter[T, J]] {
      q"""
  new Converter[$tpe, $tpj] {
    def write(t: $tpe): $tpj = {
      val javaObj = new $tpj()
      $setJavaFields
      javaObj
    }

    def read(j: $tpj): $tpe = $companion(..$fromJavaParams)
  }
  """
    }
  }
}
