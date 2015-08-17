package com.stackstate.scalajavamapper.property;

public class JavaProperty extends JavaBaseProperty {
    public JavaProperty(String name){
        this.name = name;
    }
    public JavaProperty(){
    }
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
