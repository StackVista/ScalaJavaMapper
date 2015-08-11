package com.stackstate.scalajavamapper

import com.stackstate.scalajavamapper.conversionmacros.ConverterMacro

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

trait JavaWriter[T, J] {
  def write(t: T): J
}

trait JavaReader[T, J] {
  def read(j: J): T
}

@implicitNotFound("Required Converter from scala type ${T} to java type ${J} not found.")
trait Converter[T, J] extends JavaWriter[T, J] with JavaReader[T, J]

object Converter {

  def toJava[T, J](t: T)(implicit converter: JavaWriter[T, J]): J = converter.write(t)
  def fromJava[T, J](j: J)(implicit converter: JavaReader[T, J]): T = converter.read(j)

  def converter[T, J](customFieldMapping: (String, String)*): Converter[T, J] = macro ConverterMacro.converter[T, J]
}
