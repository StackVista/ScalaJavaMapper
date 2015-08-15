package com.stackstate.scalajavamapper
package conversionmacros

import scala.reflect.macros.whitebox.Context

class FieldsConverterModule[C <: Context](val c: C) {
  import c.universe._

  class FieldsConverter(tpeCaseClass: c.universe.Type, tpeJavaClass: c.universe.Type,
      fieldMapping: FieldMapping, customConvertersMapping: Map[String, c.universe.Tree]) {

    val scalaFieldList = scalaFields(tpeCaseClass)

    def javaSetterCalls = scalaFieldList.map {
      case (scalaFieldName, decodedFieldName, scalaFieldType) ⇒
        customConvertersMapping.get(decodedFieldName).map { converter =>
          val converterName = TermName(s"${decodedFieldName}Converter")

          if (converter.tpe <:< c.symbolOf[CustomFieldReadWriter[_, _]].toType) {
            val (javaSetterName, javaSetterType) = fieldMapping.javaSetter(c)(tpeJavaClass, decodedFieldName)
            q"""
              this.$converterName.writer.foreach(writer =>
                javaObj.$javaSetterName(com.stackstate.scalajavamapper.Converter.toJava[$scalaFieldType, $javaSetterType](t.$scalaFieldName)(writer))
              )
            """
          } else q""
        }.getOrElse {
          val (javaSetterName, javaSetterType) = fieldMapping.javaSetter(c)(tpeJavaClass, decodedFieldName)
          q"""javaObj.$javaSetterName(com.stackstate.scalajavamapper.Converter.toJava[$scalaFieldType, $javaSetterType](t.$scalaFieldName))"""
        }
    }

    def caseClassConstructorArgs = scalaFieldList.map {
      case (scalaFieldName, decodedFieldName, scalaFieldType) ⇒
        val (javaGetterName, javaGetterType) = fieldMapping.javaGetter(c)(tpeJavaClass, decodedFieldName)

        customConvertersMapping.get(decodedFieldName).map { converter =>
          val converterName = TermName(s"${decodedFieldName}Converter")
          q"""com.stackstate.scalajavamapper.Converter.fromJava[$scalaFieldType, $javaGetterType](j.$javaGetterName)(this.$converterName.reader)"""
        }.getOrElse {
          q"""com.stackstate.scalajavamapper.Converter.fromJava[$scalaFieldType, $javaGetterType](j.$javaGetterName)"""
        }
    }

    private def scalaFields(tpeCaseClass: c.universe.Type): List[(c.universe.TermName, String, c.universe.Type)] = {
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
}