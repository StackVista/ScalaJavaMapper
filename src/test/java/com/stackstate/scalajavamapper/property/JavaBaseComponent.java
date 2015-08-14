package com.stackstate.scalajavamapper.property;

public abstract class JavaBaseComponent<T extends JavaBaseProperty> {
    abstract T getProperty();

    abstract void setProperty(T property);
}
