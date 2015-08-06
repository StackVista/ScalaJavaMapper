package com.stackstate

import objectmapper.macros._

object Main extends App {
  import Converter._

  case class Person(name: String, age: Option[Int])
  case class Item(name: String, price: Double, list: Seq[String], person: Person)

  implicit val personConverter = Converter.converter[Person, JavaPerson]
  implicit val itemConverter = Converter.converter[Item, JavaItem]

  val inItem = Item("Hello", 1.0, List("a", "b"), Person("henk", Some(10)))

  val javaItem = Converter.toJava[Item, JavaItem](inItem)
  println(s"Java : $javaItem")

  val newItem = Converter.fromJava[Item, JavaItem](javaItem)
  println(s"Scala after roundtrip: $newItem")
}
