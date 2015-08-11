package com.stackstate.scalajavamapper
package conversionmacros

import scala.reflect.macros.whitebox.Context

object ConverterMacro {
  def converter[T: c.WeakTypeTag, J: c.WeakTypeTag](c: Context)(customFieldMapping: c.Expr[(String, String)]*): c.Expr[Converter[T, J]] = {
    import c.universe._
    val tpeCaseClass = weakTypeOf[T]
    val tpeJavaClass = weakTypeOf[J]

    val fieldMapping = FieldMapping(c)(customFieldMapping)
    val companion = tpeCaseClass.typeSymbol.companion
    if (companion == NoSymbol) {
      c.abort(c.enclosingPosition, s"No companion companion object found for case class ${tpeCaseClass}")
    }

    val (toJavaStatements, fromJavaParams) = scalaFields(c)(tpeCaseClass).map {
      case (scalaFieldName, decodedFieldName, scalaFieldType) ⇒

        val (javaSetterName, javaSetterType) = fieldMapping.javaSetter(c)(tpeJavaClass, decodedFieldName)
        val (javaGetterName, javaGetterType) = fieldMapping.javaGetter(c)(tpeJavaClass, decodedFieldName)

        (q"""javaObj.$javaSetterName(com.stackstate.scalajavamapper.Converter.toJava[$scalaFieldType, $javaSetterType](t.$scalaFieldName))""",
          q"""com.stackstate.scalajavamapper.Converter.fromJava[$scalaFieldType, $javaGetterType](j.$javaGetterName)""")
    }.unzip

    val setJavaFields = toStatements(c)(toJavaStatements)

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

  private def toStatements(c: Context)(toJavaStatements: List[c.universe.Tree]): c.universe.Tree = {
    import c.universe._
    toJavaStatements.foldLeft(q"")((acc, next) => q"..$acc; $next")
  }

  private def scalaFields(c: Context)(tpeCaseClass: c.universe.Type): List[(c.universe.TermName, String, c.universe.Type)] = {
    import c.universe._
    val fields = tpeCaseClass.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor ⇒ m
    }.get.paramLists.head

    fields.map { field ⇒
      val scalaFieldName = field.asTerm.name
      val decodedFieldName = scalaFieldName.decodedName.toString
      val scalaFieldType = tpeCaseClass.decl(scalaFieldName).typeSignature
      (scalaFieldName, decodedFieldName, scalaFieldType)
    }
  }
}

