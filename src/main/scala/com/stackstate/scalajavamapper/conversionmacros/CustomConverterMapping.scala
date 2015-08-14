package com.stackstate.scalajavamapper
package conversionmacros

import scala.reflect.macros.whitebox.Context

object CustomConverterMapping {
  def createMapping(c: Context)(customConverterMapping: Seq[c.Expr[(String, CustomFieldConverter[_, _])]]): Map[String, c.universe.Tree] = {
    import c.universe._
    def getConstantValue(nameTree: c.Tree): String = {
      nameTree match {
        case Literal(Constant(name)) => name.toString
        case other =>
          c.abort(c.enclosingPosition, s"Could not read custom converter mapping: $other. Only String literals for fieldnames are supported.")
      }
    }

    customConverterMapping.map { tuple =>
      tuple.tree match {
        case q"scala.this.Predef.ArrowAssoc[${ _ }]($scalaName).->[${ _ }]($customConverter)" =>
          getConstantValue(scalaName) -> customConverter
        case wrongSyntax =>
          c.abort(c.enclosingPosition, s"Could not read custom field mapping: $wrongSyntax. Provide mappings with the arrow syntax: scalaName -> CustomFieldConverter")
      }
    }.toMap
  }
}

