package com.stackstate.scalajavamapper;

import java.util.Set;

public class JavaPerson {
  private String name;
  private Integer age;
  private Set<String> set;

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }


  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  @Override
  public String toString() {
    return "JavaPerson{" +
      "name='" + name + '\'' +
      ", age=" + age +
      '}';
  }

  public Set<String> getSet() {
    return set;
  }

  public void setSet(Set<String> set) {
    this.set = set;
  }
}
