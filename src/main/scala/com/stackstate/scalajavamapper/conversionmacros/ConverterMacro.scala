package com.stackstate.scalajavamapper
package conversionmacros

import scala.reflect.macros.whitebox.Context

object ConverterMacro {
  def converter[T: c.WeakTypeTag, J: c.WeakTypeTag](c: Context)(customFieldMapping: c.Expr[(String, String)]*)(customFieldConverters: c.Expr[(String, CustomFieldConverter[_, _])]*): c.Expr[Converter[T, J]] = {
    import c.universe._
    val tpeCaseClass = weakTypeOf[T]
    val tpeJavaClass = weakTypeOf[J]

    val fieldMapping = FieldMapping(c)(customFieldMapping)
    val companion = tpeCaseClass.typeSymbol.companion
    if (companion == NoSymbol) {
      c.abort(c.enclosingPosition, s"No companion companion object found for case class ${tpeCaseClass}")
    }

    val customConvertersMapping = CustomConverterMapping.createMapping(c)(customFieldConverters)

    val converterFields = customConvertersMapping.map {
      case (name, converter: c.universe.Tree) =>
        val converterName = TermName(s"${name}Converter")
        q"""val $converterName = $converter"""
    }

    val (toJavaStatements, fromJavaParams) = scalaFields(c)(tpeCaseClass).map {
      case (scalaFieldName, decodedFieldName, scalaFieldType) ⇒

        val (javaSetterName, javaSetterType) = fieldMapping.javaSetter(c)(tpeJavaClass, decodedFieldName)
        val (javaGetterName, javaGetterType) = fieldMapping.javaGetter(c)(tpeJavaClass, decodedFieldName)

        customConvertersMapping.get(decodedFieldName).map { converter =>
          val converterName = TermName(s"${decodedFieldName}Converter")
          val toJavaStatement: Tree =
            q"""
                this.$converterName.writer.foreach(writer =>
                  javaObj.$javaSetterName(com.stackstate.scalajavamapper.Converter.toJava[$scalaFieldType, $javaSetterType](t.$scalaFieldName)(writer))
                )
              """
          val fromJavaExpression: Tree = q"""com.stackstate.scalajavamapper.Converter.fromJava[$scalaFieldType, $javaGetterType](j.$javaGetterName)(this.$converterName.reader)"""
          (toJavaStatement, fromJavaExpression)
        }.getOrElse {
          val toJavaStatement: Tree =
            q"""javaObj.$javaSetterName(com.stackstate.scalajavamapper.Converter.toJava[$scalaFieldType, $javaSetterType](t.$scalaFieldName))"""
          val fromJavaExpression: Tree = q"""com.stackstate.scalajavamapper.Converter.fromJava[$scalaFieldType, $javaGetterType](j.$javaGetterName)"""
          (toJavaStatement, fromJavaExpression)
        }
    }.unzip

    c.Expr[Converter[T, J]] {
      q"""
        new Converter[$tpeCaseClass, $tpeJavaClass] {
          ..$converterFields

          def write(t: $tpeCaseClass): $tpeJavaClass = {
            val javaObj = new $tpeJavaClass()
            ..$toJavaStatements
            javaObj
          }

          def read(j: $tpeJavaClass): $tpeCaseClass = $companion(..$fromJavaParams)
        }
      """
    }
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
