package com.stackstate.scalajavamapper.conversionmacros

import scala.reflect.macros.blackbox.Context

object FieldMapping {

  def apply(c: Context)(customFieldMapping: Seq[c.Expr[(String, String)]]): FieldMapping = {
    import c.universe._
    def getConstantValue(nameTree: c.Tree): String = {
      nameTree match {
        case Literal(Constant(name)) => name.toString
        case other =>
          c.abort(c.enclosingPosition, s"Could not read custom field mapping: $other. Only String literals for fieldnames are supported.")
      }
    }
    val mapping = customFieldMapping.map { tuple =>
      tuple.tree match {
        case q"scala.this.Predef.ArrowAssoc[${ _ }]($scalaName).->[${ _ }]($javaName)" =>
          getConstantValue(scalaName) -> getConstantValue(javaName)
        case wrongSyntax =>
          c.abort(c.enclosingPosition, s"Could not read custom field mapping: $wrongSyntax. Provide mappings with the arrow syntax: scalaName -> javaName")
      }
    }.toMap

    FieldMapping(mapping)
  }
}

case class FieldMapping(mapping: Map[String, String]) {
  private def mapField(name: String): String = mapping.getOrElse(name, name)

  def javaGetter(c: Context)(tpeJavaClass: c.universe.Type, fieldName: String): (c.universe.TermName, c.universe.Type) = {
    val javaProperty: String = mapField(fieldName)

    def matches(prefix: String)(member: c.universe.Symbol) = {
      member.asTerm.name.decodedName.toString == s"$prefix${javaProperty.capitalize}"
    }
    def isGetter = matches("get")_
    def isBooleanGetter(member: c.universe.Symbol) = {
      member.typeSignature.resultType =:= c.symbolOf[Boolean].toType && matches("is")(member)
    }

    val javaGetter = tpeJavaClass.members.find(member => isGetter(member) || isBooleanGetter(member))
      .getOrElse(
        c.abort(c.enclosingPosition, s"Getter for property $javaProperty not found on ${tpeJavaClass.typeSymbol.name}.")
      )

    val javaGetterName = javaGetter.asTerm.name
    val javaGetterType = javaGetter.typeSignatureIn(tpeJavaClass).resultType
    (javaGetterName, javaGetterType)
  }

  def javaSetter(c: Context)(tpeJavaClass: c.universe.Type, fieldName: String): (c.universe.TermName, c.universe.Type) = {
    val setterName = s"set${mapField(fieldName).capitalize}"
    val javaSetter = tpeJavaClass.members.find(_.asTerm.name.decodedName.toString == setterName).getOrElse(
      c.abort(c.enclosingPosition, s"Setter $setterName not found on ${tpeJavaClass.typeSymbol.name}.")
    )
    val javaSetterName = javaSetter.asTerm.name
    val javaSetterParams = javaSetter.typeSignatureIn(tpeJavaClass).paramLists.head
    if (javaSetterParams.size != 1) {
      c.abort(c.enclosingPosition, s"Setter ${javaSetterName} requires exactly one parameter, found ${javaSetterParams.size}")
    }
    val javaSetterType = javaSetterParams.head.typeSignature
    (javaSetterName, javaSetterType)
  }
}
