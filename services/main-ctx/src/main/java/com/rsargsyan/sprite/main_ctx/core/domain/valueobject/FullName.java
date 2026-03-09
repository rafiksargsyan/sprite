package com.rsargsyan.sprite.main_ctx.core.domain.valueobject;

import com.rsargsyan.sprite.main_ctx.core.exception.InvalidFullNameException;

public record FullName(String value) {
  public static final int MAX_LENGTH = 127;
  public FullName(String value) {
    this.value = validate(value);
  }

  public static String validate(String value) {
    if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
      throw new InvalidFullNameException();
    }
    return value.trim();
  }

  public static FullName fromString(String value) {
    if (value == null || value.isBlank()) return null;
    return new FullName(value);
  }
}
