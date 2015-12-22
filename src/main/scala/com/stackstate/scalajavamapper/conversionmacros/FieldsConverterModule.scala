package com.stackstate.scalajavamapper
package conversionmacros

import scala.reflect.macros.blackbox.Context

class FieldsConverterModule[C <: Context](val c: C) {
  import c.universe._

  class FieldsConverter(tpeCaseClass: c.universe.Type, tpeJavaClass: c.universe.Type,
      fieldMapping: FieldMapping, customConvertersMapping: Map[String, c.universe.Tree]) {

    val scalaFieldList = scalaFields(tpeCaseClass)

    def javaSetterCalls = scalaFieldList.map {
      case (scalaFieldName, decodedFieldName, scalaFieldType) ⇒
        customConvertersMapping.get(decodedFieldName).map { converter =>
          val converterName = TermName(s"${decodedFieldName}Converter")

          if (converter.tpe.typeConstructor <:< c.symbolOf[CustomFieldReadWriter[_, _]].toType.typeConstructor) {
            val (javaSetterName, javaSetterType) = fieldMapping.javaSetter(c)(tpeJavaClass, decodedFieldName)
            q"""
              javaObj.$javaSetterName(this.$converterName.writer.write(t.$scalaFieldName))
            """
          } else {
            q""""""
          }
        }.getOrElse {
          val (javaSetterName, javaSetterType) = fieldMapping.javaSetter(c)(tpeJavaClass, decodedFieldName)
          q"""javaObj.$javaSetterName(com.stackstate.scalajavamapper.toJava[$scalaFieldType, $javaSetterType](t.$scalaFieldName))"""
        }
    }

    def caseClassConstructorArgs = scalaFieldList.map {
      case (scalaFieldName, decodedFieldName, scalaFieldType) ⇒
        val (javaGetterName, javaGetterType) = fieldMapping.javaGetter(c)(tpeJavaClass, decodedFieldName)

        customConvertersMapping.get(decodedFieldName).map { converter =>
          val converterName = TermName(s"${decodedFieldName}Converter")
          q"""this.$converterName.reader.read(j.$javaGetterName)"""
        }.getOrElse {
          q"""com.stackstate.scalajavamapper.fromJava[$scalaFieldType, $javaGetterType](j.$javaGetterName)"""
        }
    }

    private def scalaFields(tpeCaseClass: c.universe.Type): List[(c.universe.TermName, String, c.universe.Type)] = {
      val fields = tpeCaseClass.decls.collectFirst {
        case m: MethodSymbol if m.isPrimaryConstructor ⇒ m
      }.getOrElse(c.abort(c.enclosingPosition, s"No constructor found. Maybe ${tpeCaseClass} is not a case class?"))
        .paramLists.head

      fields.map { field ⇒
        val scalaFieldName = field.asTerm.name
        val decodedFieldName = scalaFieldName.decodedName.toString
        val scalaFieldType = tpeCaseClass.decl(scalaFieldName).typeSignature
        (scalaFieldName, decodedFieldName, scalaFieldType)
      }
    }
  }
}
