package com.stackstate.scalajavamapper.property;

public abstract class JavaBaseComponent<T extends JavaBaseProperty> {
    protected T property;

    public T getProperty() {
      return property;
    }

    public void setProperty(T property) {
      this.property = property;
    }
}
