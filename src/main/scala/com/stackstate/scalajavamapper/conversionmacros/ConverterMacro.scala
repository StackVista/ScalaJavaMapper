package com.stackstate.scalajavamapper
package conversionmacros

import scala.reflect.macros.blackbox.Context

object ConverterMacro {
  def reader[T: c.WeakTypeTag, J: c.WeakTypeTag](c: Context)(customFieldMapping: c.Expr[(String, String)]*)(customFieldConverters: c.Expr[(String, CustomFieldConverter[_, _])]*): c.Expr[JavaReader[T, J]] = {
    val generatorModule = new CodeGeneratorModule[c.type](c)
    val generator = new generatorModule.ConverterGenerator[T, J](customFieldMapping, customFieldConverters)
    generator.reader
  }

  def writer[T: c.WeakTypeTag, J: c.WeakTypeTag](c: Context)(customFieldMapping: c.Expr[(String, String)]*)(customFieldConverters: c.Expr[(String, CustomFieldConverter[_, _])]*): c.Expr[JavaWriter[T, J]] = {
    val generatorModule = new CodeGeneratorModule[c.type](c)
    val generator = new generatorModule.ConverterGenerator[T, J](customFieldMapping, customFieldConverters)
    generator.writer
  }

  def converter[T: c.WeakTypeTag, J: c.WeakTypeTag](c: Context)(customFieldMapping: c.Expr[(String, String)]*)(customFieldConverters: c.Expr[(String, CustomFieldConverter[_, _])]*): c.Expr[Converter[T, J]] = {
    val generatorModule = new CodeGeneratorModule[c.type](c)
    val generator = new generatorModule.ConverterGenerator[T, J](customFieldMapping, customFieldConverters)
    generator.converter
  }
}

class CodeGeneratorModule[C <: Context](override val c: C) extends FieldsConverterModule[C](c) {

  class ConverterGenerator[T: c.WeakTypeTag, J: c.WeakTypeTag](customFieldMapping: Seq[c.Expr[(String, String)]], customFieldConverters: Seq[c.Expr[(String, CustomFieldConverter[_, _])]]) {
    import c.universe._

    def converter: c.Expr[Converter[T, J]] = {
      c.Expr[Converter[T, J]] {
        q"""
        new Converter[$tpeCaseClass, $tpeJavaClass] {
          ..$converterFields

          def write(t: $tpeCaseClass): $tpeJavaClass = {
            if (t == null) null
              else {val javaObj = new $tpeJavaClass()
              ..${fieldsConverter.javaSetterCalls}
              javaObj
            }
          }

          def read(j: $tpeJavaClass): $tpeCaseClass = {
            if (j == null) null
            else $companion(..${fieldsConverter.caseClassConstructorArgs})
          }
        }
      """
      }
    }

    def reader: c.Expr[JavaReader[T, J]] = {
      c.Expr[JavaReader[T, J]] {
        q"""
        new JavaReader[$tpeCaseClass, $tpeJavaClass] {
          ..$converterFields

          def read(j: $tpeJavaClass): $tpeCaseClass = {
            if (j == null) null
            else $companion(..${fieldsConverter.caseClassConstructorArgs})
          }
        }
      """
      }
    }

    def writer: c.Expr[JavaWriter[T, J]] = {
      c.Expr[JavaWriter[T, J]] {
        q"""
        new JavaWriter[$tpeCaseClass, $tpeJavaClass] {
          ..$converterFields

          def write(t: $tpeCaseClass): $tpeJavaClass = {
            if (t == null) null
            else {
              val javaObj = new $tpeJavaClass()
              ..${fieldsConverter.javaSetterCalls}
              javaObj
            }
          }
        }
      """
      }
    }

    private val tpeCaseClass = weakTypeOf[T]
    private val tpeJavaClass = weakTypeOf[J]

    ensureCaseClass(tpeCaseClass)

    private val fieldMapping = FieldMapping(c)(customFieldMapping)
    private val customConverters = CustomConverterMapping.createMapping(c)(customFieldConverters)

    private val converterFields = customConverters.map {
      case (name, converter: c.universe.Tree) =>
        val converterName = TermName(s"${name}Converter")
        q"""val $converterName = $converter"""
    }

    private val fieldsConverter = new FieldsConverter(tpeCaseClass, tpeJavaClass, fieldMapping, customConverters)

    private def companion = {
      val companion = tpeCaseClass.typeSymbol.companion
      if (companion == NoSymbol) {
        c.abort(c.enclosingPosition, s"No companion companion object found for case class ${tpeCaseClass}")
      }
      companion
    }

    private def ensureCaseClass(tpe: Type) = if (!tpe.typeSymbol.asClass.isCaseClass) c.abort(c.enclosingPosition, s"$tpe is not a case class.")
  }
}
