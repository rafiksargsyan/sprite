package com.rsargsyan.sprite.main_ctx.core.domain.valueobject;

public record Name(String value) {
  public Name(String value) {
    this.value = validate(value);
  }

  public static String validate(String value) {
    return value; //TODO: implement validation
  }

}
