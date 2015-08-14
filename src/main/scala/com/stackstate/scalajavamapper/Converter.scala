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

object JavaReader {
  def apply[T, J](f: J => T) = new JavaReader[T, J] {
    override def read(j: J): T = f(j)
  }
}

object JavaWriter {
  def apply[T, J](f: T => J) = new JavaWriter[T, J] {
    override def write(t: T): J = f(t)
  }
}

@implicitNotFound("Required Converter from scala type ${T} to java type ${J} not found.")
trait Converter[T, J] extends JavaWriter[T, J] with JavaReader[T, J]

case class CustomFieldConverter[T, J](reader: JavaReader[T, J], writer: Option[JavaWriter[T, J]])

object Converter {

  def customField[T, J](read: J => T, write: T => J): CustomFieldConverter[T, J] = CustomFieldConverter[T, J](JavaReader(read), Some(JavaWriter(write)))
  def readonlyField[T, J](read: J => T): CustomFieldConverter[T, J] = CustomFieldConverter[T, J](JavaReader(read), None)

  def toJava[T, J](t: T)(implicit converter: JavaWriter[T, J]): J = converter.write(t)
  def fromJava[T, J](j: J)(implicit converter: JavaReader[T, J]): T = converter.read(j)

  def converter[T, J](customFieldMapping: (String, String)*)(customFieldConverters: (String, CustomFieldConverter[_, _])*): Converter[T, J] = macro ConverterMacro.converter[T, J]
}
