package com.stackstate.scalajavamapper

//import shapeless.{ HNil, HList }
//
//import scala.reflect.ClassTag
//
//sealed trait ApiDescriptor
//case class Read[S, J]() extends ApiDescriptor
//case class Write[S, J]() extends ApiDescriptor
//case class ReadWrite[S, J]() extends ApiDescriptor
//
//trait Api {
//  def read[S, J]
//  def write[S, J]
//  def readWrite[S, J]
//
//  def process
//}
//
//case class SomeApi(descriptors: HList) extends ApiDescriptor {
//  def read[S, J] = copy(Read[S, J]() :: descriptors)
//  def write[S, J] = copy(Write[S, J]() :: descriptors)
//  def readWrite[S, J] = copy(ReadWrite[S, J]() :: descriptors)
//}
//
//object EmptyApi {
//  def read[S, J] = SomeApi(Read[S, J]() :: HNil)
//  def write[S, J] = SomeApi(Write[S, J]() :: HNil)
//  def readWrite[S, J] = SomeApi(ReadWrite[S, J]() :: HNil)
//}
//
//object Test {
//
//  def readConverter[S, J](read: Read[S, J])(implicit converter: Converter[S, J]) = converter
//
//  def classTags[S, J](read: Read[S, J])(implicit tagT: ClassTag[S], tagJ: ClassTag[J]) = (tagT, tagJ)
//
//}
