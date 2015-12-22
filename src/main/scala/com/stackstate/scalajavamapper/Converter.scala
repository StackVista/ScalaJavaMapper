package com.stackstate

import scala.annotation.implicitNotFound

package scalajavamapper {
  @implicitNotFound("Required JavaWriter from scala type ${T} to java type ${J} not found.")
  trait JavaWriter[T, J] {
    def write(t: T): J
  }

  @implicitNotFound("Required JavaReader from scala type ${T} to java type ${J} not found.")
  trait JavaReader[T, J] {
    def read(j: J): T
  }

  @implicitNotFound("Required Converter from scala type ${T} to java type ${J} not found.")
  trait Converter[T, J] extends JavaWriter[T, J] with JavaReader[T, J]

  object JavaReader {
    def apply[T, J](f: J => T): JavaReader[T, J] = new JavaReader[T, J] {
      override def read(j: J): T = f(j)
    }
  }

  object JavaWriter {
    def apply[T, J](f: T => J): JavaWriter[T, J] = new JavaWriter[T, J] {
      override def write(t: T): J = f(t)
    }
  }

  object Converter {
    def apply[T, J](write: T => J, read: J => T): Converter[T, J] = new Converter[T, J] {
      override def write(t: T): J = write(t)
      override def read(j: J): T = read(j)
    }
  }

  trait CustomFieldConverter[T, J]

  case class CustomFieldReaderOnly[T, J](reader: JavaReader[T, J]) extends CustomFieldConverter[T, J]
  case class CustomFieldReadWriter[T, J](reader: JavaReader[T, J], writer: JavaWriter[T, J]) extends CustomFieldConverter[T, J]
}

package object scalajavamapper {
  def readOnlyField[T, J](read: J => T): CustomFieldReaderOnly[T, J] = CustomFieldReaderOnly[T, J](JavaReader(read))
  def readWriterField[T, J](read: J => T, write: T => J): CustomFieldReadWriter[T, J] = CustomFieldReadWriter[T, J](JavaReader(read), JavaWriter(write))
  def converterField[T, J](converter: Converter[T, J]): CustomFieldReadWriter[T, J] = CustomFieldReadWriter[T, J](converter, converter)

  def toJava[T, J](t: T)(implicit converter: JavaWriter[T, J]): J = converter.write(t)
  def fromJava[T, J](j: J)(implicit converter: JavaReader[T, J]): T = converter.read(j)
}
