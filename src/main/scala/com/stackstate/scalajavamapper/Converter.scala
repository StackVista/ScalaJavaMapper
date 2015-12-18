package com.stackstate.scalajavamapper

import com.stackstate.scalajavamapper.conversionmacros.ConverterMacro
import com.stackstate.scalajavamapper.conversionmacros.AutoConverterMacro

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

trait CustomFieldConverter[T, J]

case class CustomFieldReaderOnly[T, J](reader: JavaReader[T, J]) extends CustomFieldConverter[T, J]
case class CustomFieldReadWriter[T, J](reader: JavaReader[T, J], writer: JavaWriter[T, J]) extends CustomFieldConverter[T, J]

trait BaseMapper[BaseT, BaseJ] {
  def readOnlyField[T <: BaseT, J <: BaseJ](read: J => T): CustomFieldReaderOnly[T, J] = CustomFieldReaderOnly[T, J](JavaReader(read))
  def readWriterField[T <: BaseT, J <: BaseJ](read: J => T, write: T => J): CustomFieldReadWriter[T, J] = CustomFieldReadWriter[T, J](JavaReader(read), JavaWriter(write))
  def converterField[T <: BaseT, J <: BaseJ](converter: Converter[T, J]): CustomFieldReadWriter[T, J] = CustomFieldReadWriter[T, J](converter, converter)

  def toJava[T <: BaseT, J <: BaseJ](t: T)(implicit converter: JavaWriter[T, J]): J = converter.write(t)
  def fromJava[T <: BaseT, J <: BaseJ](j: J)(implicit converter: JavaReader[T, J]): T = converter.read(j)
}

trait AutoMapper[BaseT, BaseJ] extends BaseMapper[BaseT, BaseJ] {
  implicit def createConverter[T <: BaseT, J <: BaseJ]: Converter[T, J] = macro AutoConverterMacro.converter[T, J]
  implicit def createReader[T <: BaseT, J <: BaseJ]: JavaReader[T, J] = macro AutoConverterMacro.reader[T, J]
  implicit def createWriter[T <: BaseT, J <: BaseJ]: JavaWriter[T, J] = macro AutoConverterMacro.writer[T, J]
}

trait Mapper[BaseT, BaseJ] extends BaseMapper[BaseT, BaseJ] {
  def createConverter[T <: BaseT, J <: BaseJ](customFieldMapping: (String, String)*)(customFieldConverters: (String, CustomFieldConverter[_, _])*): Converter[T, J] = macro ConverterMacro.converter[T, J]
  def createReader[T <: BaseT, J <: BaseJ](customFieldMapping: (String, String)*)(customFieldConverters: (String, CustomFieldConverter[_, _])*): JavaReader[T, J] = macro ConverterMacro.reader[T, J]
  def createWriter[T <: BaseT, J <: BaseJ](customFieldMapping: (String, String)*)(customFieldConverters: (String, CustomFieldConverter[_, _])*): JavaWriter[T, J] = macro ConverterMacro.writer[T, J]
}

object Mapper extends Mapper[Any, Any]
