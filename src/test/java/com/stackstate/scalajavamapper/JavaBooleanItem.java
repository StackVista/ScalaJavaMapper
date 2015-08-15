package com.stackstate.scalajavamapper;

public class JavaBooleanItem {
  private boolean known;
  private Boolean familiar;

  public JavaBooleanItem() {}

  public JavaBooleanItem(boolean known, Boolean familiar) {
    this.known = known;
    this.familiar = familiar;
  }

  public boolean isKnown() {
    return known;
  }

  public void setKnown(boolean known) {
    this.known = known;
  }

  public Boolean getFamiliar() {
    return familiar;
  }

  public void setFamiliar(Boolean familiar) {
    this.familiar = familiar;
  }
}
