package com.stackstate.scalajavamapper

import java.util

import property._
import org.scalatest._
import Converters._
import Mapper._

class MacroTest extends WordSpecLike with Matchers {
  import Converters._
  import Mapper._

  case class Person(name: String, age: Option[Int], set: Set[String])

  case class PersonSimple(age: Int)

  case class OtherPerson(fullName: String, age1: Int, settings: Set[String])

  case class Item(name: String, price: Double, list: Seq[String], person: Person)

  case class ItemSimple(person: String)

  case class BooleanItem(known: Boolean, familiar: Boolean)

  case class Property(name: String)

  case class Component(property: Property)

  class NoCaseClass(val name: String) {}

  val inItem = Item("String", 1.0, List("a", "b"), Person("John", Some(10), Set("d", "e", "f")))

  val inJavaPerson = new JavaPerson("test")
  inJavaPerson.setAge(1)
  inJavaPerson.setSet(new util.HashSet())
  val javaList = new util.ArrayList[String]()
  javaList.add("a")
  javaList.add("b")
  val inJavaItem = new JavaItem("String", 1.0d, javaList, inJavaPerson)

  "The converter macro" should {
    "convert a case class to a Java object" in {
      implicit val personConverter = createConverter[Person, JavaPerson]()()
      implicit val itemConverter = createConverter[Item, JavaItem]()()

      val javaItem = toJava[Item, JavaItem](inItem)
      inItem.name === javaItem.getName
      inItem.price === javaItem.getPrice
      javaItem.getList should contain theSameElementsInOrderAs Vector("a", "b")
      inItem.person.name === javaItem.getPerson.getName
      inItem.person.age.get === javaItem.getPerson.getAge
      javaItem.getPerson.getSet should contain theSameElementsAs Set("d", "e", "f")

      val newItem = fromJava[Item, JavaItem](javaItem)
      inItem === newItem
    }

    "convert and map field names to other field names if provided" in {
      implicit val personConverter = createConverter[OtherPerson, JavaPerson](
        "fullName" -> "name", "age1" -> "age", "settings" -> "set"
      )()

      val inPerson = OtherPerson("John", 10, Set("d", "e", "f"))

      val javaPerson = toJava[OtherPerson, JavaPerson](inPerson)
      inPerson.fullName === javaPerson.getName
      inPerson.age1 === javaPerson.getAge
      javaPerson.getSet should contain theSameElementsAs Set("d", "e", "f")

      val newOtherPerson = fromJava[OtherPerson, JavaPerson](javaPerson)
      inPerson == newOtherPerson
    }

    "convert nested properties with the specified custom converters" in {
      implicit val personConverter = createConverter[Person, JavaPerson]()()
      implicit val itemConverter = createConverter[Item, JavaItem]()()
      val javaItem = toJava[Item, JavaItem](inItem)

      implicit val simpleItemConverter = createConverter[ItemSimple, JavaItem]()(
        "person" -> readWriterField[String, JavaPerson](_.getName, personName => new JavaPerson(personName))
      )

      val simpleItem = fromJava[ItemSimple, JavaItem](javaItem)
      simpleItem.person === inItem.person.name

      val newJavaItem = toJava[ItemSimple, JavaItem](simpleItem)
      newJavaItem.getPerson.getName === inItem.person.name
    }

    "convert nested properties with the specified readonly converter" in {
      implicit val personConverter = createConverter[Person, JavaPerson]()()
      implicit val itemConverter = createConverter[Item, JavaItem]()()
      val javaItem = toJava[Item, JavaItem](inItem)

      implicit val simpleItemConverter = createConverter[ItemSimple, JavaItem]()(
        "person" -> readOnlyField((_: JavaPerson).getName)
      )

      val simpleItem = fromJava[ItemSimple, JavaItem](javaItem)
      simpleItem.person === inItem.person.name

      val newJavaItem = toJava[ItemSimple, JavaItem](simpleItem)
      newJavaItem.getPerson.getName === "default-name"
    }

    "convert readonly properties without setter converter" in {
      implicit val personConverter = createConverter[PersonSimple, JavaPersonSimple]()("age" -> readOnlyField[Int, Integer](_.asInstanceOf[Integer]))

      val scalaPerson = PersonSimple(0)
      val javaPerson = toJava[PersonSimple, JavaPersonSimple](scalaPerson)
      javaPerson.getAge === 1

      val returnDto = fromJava[PersonSimple, JavaPersonSimple](javaPerson)
      returnDto.age === 1
    }

    "convert boolean properties with 'is' getters" in {
      implicit val boolItemConverter = createConverter[BooleanItem, JavaBooleanItem]()()
      val scalaItem = BooleanItem(true, false)
      val javaItem = toJava[BooleanItem, JavaBooleanItem](scalaItem)

      javaItem.isKnown === scalaItem.known
      javaItem.getFamiliar === scalaItem.familiar

      val outScalaItem = fromJava[BooleanItem, JavaBooleanItem](javaItem)
      outScalaItem === scalaItem
    }

    "handle 'null' values gracefully" in {
      implicit val personConverter = createConverter[Person, JavaPerson]()()
      implicit val itemConverter = createConverter[Item, JavaItem]()()

      val javaItem = toJava[Item, JavaItem](null)
      javaItem === null

      val newItem = fromJava[Item, JavaItem](null)
      newItem === null
    }

    "give a compilation error if the Scala class is not a case class" in {
      "val personConverter = createConverter[NoCaseClass, JavaPerson]()()" shouldNot compile
    }

    "convert base class with generic type" in {
      val propertyConverter = createConverter[Property, JavaProperty]()()

      implicit val componentConverter = createConverter[Component, JavaComponent]()(
        "property" -> converterField[Property, JavaProperty](propertyConverter)
      )

      val dto = Component(Property("name"))

      val domain = toJava[Component, JavaComponent](dto)

      domain.getProperty.getName === "name"
    }
  }

  "The reader macro" should {
    "only be able to read from a Java object" in {
      implicit val personReader = createReader[Person, JavaPerson]()()
      implicit val itemReader = createReader[Item, JavaItem]()()

      val newItem = fromJava[Item, JavaItem](inJavaItem)
      newItem.list === inJavaItem.getList
      newItem.name === inJavaItem.getName
      newItem.price === inJavaItem.getPrice
      newItem.person.name === inJavaPerson.getName
      newItem.person.age === inJavaPerson.getAge
      newItem.person.set === inJavaPerson.getSet

      "toJava[Item, JavaItem](newItem)" shouldNot compile
    }

    "handle 'null' values gracefully" in {
      implicit val perosnReader = createReader[Person, JavaPerson]()()
      implicit val itemReader = createReader[Item, JavaItem]()()

      val newItem = fromJava[Item, JavaItem](null)
      newItem === null
    }
  }

  "The writer macro" should {
    val propertyConverter = createConverter[Property, JavaProperty]()()
    "only be able to write to a Java object" in {
      implicit val personWriter = createWriter[Person, JavaPerson]()()
      implicit val itemWriter = createWriter[Item, JavaItem]()()

      val javaItem = toJava[Item, JavaItem](inItem)
      inItem.name === javaItem.getName
      inItem.price === javaItem.getPrice
      javaItem.getList should contain theSameElementsInOrderAs Vector("a", "b")
      inItem.person.name === javaItem.getPerson.getName
      inItem.person.age.get === javaItem.getPerson.getAge
      javaItem.getPerson.getSet should contain theSameElementsAs Set("d", "e", "f")
      implicit val componentConverter = createConverter[Component, JavaComponent]()(
        "property" -> converterField[Property, JavaProperty](propertyConverter)
      )

      "fromJava[Item, JavaItem](javaItem)" shouldNot compile
    }
    val dto = Component(Property("name"))

    "handle 'null' values gracefully" in {
      implicit val personWriter = createWriter[Person, JavaPerson]()()
      implicit val itemWriter = createWriter[Item, JavaItem]()()

      val newItem = toJava[Item, JavaItem](null)
      newItem === null
    }
  }

}
