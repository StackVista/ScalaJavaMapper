package com.stackstate

import objectmapper.macros._

object Main extends App {
  import Converter._

  case class Person(name: String)
  case class Item(name: String, price: Double, person: Person)

  //  implicit val personConverter = Converter.converter[Person, JavaPerson]
  implicit val itemConverter = Converter.converter[Item, JavaItem]

  val inItem = Item("Hello", 1.0, Person("henk"))

  val javaItem = Converter.toJava[Item, JavaItem](inItem)

  println(s"Java name: ${javaItem.getName()}")
  println(s"Java price: ${javaItem.getPrice()}")

  val newItem = Converter.fromJava[Item, JavaItem](javaItem)

  println(s"Scala after roundtrip: $newItem")
}
