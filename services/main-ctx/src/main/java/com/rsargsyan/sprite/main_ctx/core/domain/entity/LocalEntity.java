package com.rsargsyan.sprite.main_ctx.core.domain.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class LocalEntity {
  @Id
  @GeneratedValue
  private Long id;
}
