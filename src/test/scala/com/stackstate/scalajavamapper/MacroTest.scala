package com.stackstate.scalajavamapper

import org.scalatest._

class MacroTest extends WordSpecLike with Matchers {
  import Converters._

  case class Person(name: String, age: Option[Int], set: Set[String])
  case class Item(name: String, price: Double, list: Seq[String], person: Person)

  "The converter macro" should {
    "convert a case class to a Java object" in {
      implicit val personConverter = Converter.converter[Person, JavaPerson]
      implicit val itemConverter = Converter.converter[Item, JavaItem]

      val inItem = Item("String", 1.0, List("a", "b"), Person("John", Some(10), Set("d", "e", "f")))

      val javaItem = Converter.toJava[Item, JavaItem](inItem)
      inItem.name === javaItem.getName
      inItem.price === javaItem.getPrice
      inItem.list should contain theSameElementsInOrderAs Vector("a", "b")
      inItem.person.name === javaItem.getPerson.getName
      inItem.person.age.get === javaItem.getPerson.getAge
      inItem.person.set should contain theSameElementsAs Set("d", "e", "f")

      val newItem = Converter.fromJava[Item, JavaItem](javaItem)
      inItem === newItem
    }
  }
}
