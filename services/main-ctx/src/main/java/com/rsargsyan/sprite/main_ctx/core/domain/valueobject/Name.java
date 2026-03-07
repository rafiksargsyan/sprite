package com.rsargsyan.sprite.main_ctx.core.domain.valueobject;

import lombok.Value;

@Value
public final class Name {
  private final String value;

  public Name(String value) {
    this.value = validate(value);
  }

  public static String validate(String value) {
    return value; //TODO: implement validation
  }

}
