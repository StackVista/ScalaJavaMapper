package com.stackstate.scalajavamapper;
import java.util.List;

public class JavaItem {
  private String name;
  private double price;
  private List<String> list;
  private JavaPerson person;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name=name;
  }

  public double getPrice() {
    return price;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  public List<String> getList() {
    return this.list;
  }

  public void setList(List<String> list) {
    this.list = list;
  }

  public JavaPerson getPerson() {
    return person;
  }

  public void setPerson(JavaPerson person) {
    this.person = person;
  }

  @Override
  public String toString() {
    return "JavaItem{" +
      "name='" + name + '\'' +
      ", price=" + price +
      ", list=" + list +
      ", person=" + person +
      '}';
  }
}
