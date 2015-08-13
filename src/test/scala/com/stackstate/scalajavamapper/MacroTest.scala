package com.stackstate.scalajavamapper

import org.scalatest._
import CustomFieldConverter._

class MacroTest extends WordSpecLike with Matchers {
  import Converters._

  case class Person(name: String, age: Option[Int], set: Set[String])
  case class OtherPerson(fullName: String, age1: Int, settings: Set[String])

  case class Item(name: String, price: Double, list: Seq[String], person: Person)
  case class Light(statusValue: String)

  "The converter macro" should {
    "convert a case class to a Java object" in {
      implicit val personConverter = Converter.converter[Person, JavaPerson]()
      implicit val itemConverter = Converter.converter[Item, JavaItem]()

      val inItem = Item("String", 1.0, List("a", "b"), Person("John", Some(10), Set("d", "e", "f")))

      val javaItem = Converter.toJava[Item, JavaItem](inItem)
      inItem.name === javaItem.getName
      inItem.price === javaItem.getPrice
      javaItem.getList should contain theSameElementsInOrderAs Vector("a", "b")
      inItem.person.name === javaItem.getPerson.getName
      inItem.person.age.get === javaItem.getPerson.getAge
      javaItem.getPerson.getSet should contain theSameElementsAs Set("d", "e", "f")

      val newItem = Converter.fromJava[Item, JavaItem](javaItem)
      inItem === newItem
    }

    "convert and map field names to other field names if provided" in {
      implicit val personConverter = Converter.converter[OtherPerson, JavaPerson]("fullName" -> convert("name"), "age1" -> convert("age"), "settings" -> convert("set"))()

      val inPerson = OtherPerson("John", 10, Set("d", "e", "f"))

      val javaPerson = Converter.toJava[OtherPerson, JavaPerson](inPerson)
      inPerson.fullName === javaPerson.getName
      inPerson.age1 === javaPerson.getAge
      javaPerson.getSet should contain theSameElementsAs Set("d", "e", "f")

      val newOtherPerson = Converter.fromJava[OtherPerson, JavaPerson](javaPerson)
      inPerson == newOtherPerson
    }

    "convert nested properties" in {
      val statusConverter = new JavaReader[String, JavaInnerStatus] {
        override def read(j: JavaInnerStatus): String = j.getValue
      }

      implicit val lightConverter = Converter.converter[Light, JavaLight]()

      val light = Light("green")

      val javaLight: JavaLight = Converter.toJava[Light, JavaLight](light)

      javaLight.getStatus.getValue === light.statusValue
    }
  }

}

//implicit val personConverter = Converter.converter[OtherPerson, JavaPerson]("fullName" -> "name", "age1" -> "age", "settings" -> "set")
//("statusValue" -> convert("status", statusConverter))
//  convert("status", statusConverter)
