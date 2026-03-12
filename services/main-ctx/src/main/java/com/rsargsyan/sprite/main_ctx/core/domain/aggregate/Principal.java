package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.FullName;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.NameConverter;
import com.rsargsyan.sprite.main_ctx.core.exception.BlankExternalIdException;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
public class Principal extends AggregateRoot {
  @Column(name = "external_id", unique = true)
  @Getter
  private String externalId;

  @Getter
  @Column(length = FullName.MAX_LENGTH)
  @Convert(converter = NameConverter.class)
  private FullName fullName;

  @SuppressWarnings("unused")
  Principal() {}

  public Principal(String externalId, String fullName) {
    this.externalId = validateExternalId(externalId);
    this.fullName = FullName.fromString(fullName);
  }

  private String validateExternalId(String id) {
    if (id == null || id.isBlank()) throw new BlankExternalIdException();
    return id;
  }

}
