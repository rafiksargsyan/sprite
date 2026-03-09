package com.rsargsyan.sprite.main_ctx.core.domain.valueobject;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class NameConverter implements AttributeConverter<FullName, String> {

  @Override
  public String convertToDatabaseColumn(FullName attribute) {
    if (attribute == null) return null;
    return attribute.value();
  }

  @Override
  public FullName convertToEntityAttribute(String dbData) {
    return FullName.fromString(dbData);
  }
}
