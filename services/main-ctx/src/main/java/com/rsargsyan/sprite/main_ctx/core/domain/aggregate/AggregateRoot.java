package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AggregateRoot {
  @Id
  @Tsid
  private Long id;
}
