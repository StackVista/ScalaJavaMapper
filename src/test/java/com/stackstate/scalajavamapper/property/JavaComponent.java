package com.stackstate.scalajavamapper.property;

public class JavaComponent extends JavaBaseComponent<JavaProperty> {
    private JavaProperty property;

    @Override
    public JavaProperty getProperty() {
        return property;
    }

    @Override
    public void setProperty(JavaProperty property) {
        this.property = property;
    }
}
