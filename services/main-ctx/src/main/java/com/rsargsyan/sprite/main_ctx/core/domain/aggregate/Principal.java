package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Principal extends AggregateRoot {
  @Column(name = "external_id", unique = true)
  private String externalId;

  protected Principal() {}

  public Principal(String externalId) {
    this.externalId = externalId;
  }
}
