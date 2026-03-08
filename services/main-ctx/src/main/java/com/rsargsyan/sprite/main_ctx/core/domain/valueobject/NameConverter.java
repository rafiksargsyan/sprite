package com.rsargsyan.sprite.main_ctx.core.domain.valueobject;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class NameConverter implements AttributeConverter<Name, String> {

  @Override
  public String convertToDatabaseColumn(Name attribute) {
    return attribute.value();
  }

  @Override
  public Name convertToEntityAttribute(String dbData) {
    return new Name(dbData);
  }
}
