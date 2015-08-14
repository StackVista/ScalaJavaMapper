package com.stackstate.scalajavamapper;

import java.util.Set;


abstract class BaseClass {
  private String name = "default-name";

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}

public class JavaPerson extends BaseClass {
  private Integer age;

  private Set<String> set;

  @Override
  public String toString() {
    return "JavaPerson{" +
      "name='" + super.getName() + '\'' +
      ", age=" + age +
      '}';
  }

  public JavaPerson() {}

  public JavaPerson(String name) {
    this.setName(name);
  }

  public Set<String> getSet() {
    return set;
  }

  public void setSet(Set<String> set) {
    this.set = set;
  }

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }
}

