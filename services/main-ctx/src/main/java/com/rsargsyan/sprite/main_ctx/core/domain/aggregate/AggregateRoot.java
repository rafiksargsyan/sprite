package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import io.hypersistence.tsid.TSID;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

@MappedSuperclass
public abstract class AggregateRoot {
  @Id
  @Tsid
  @Getter
  private Long id;

  @Version
  private Long version;

  @Column(name = "updated_at")
  @Getter
  private Instant updatedAt;

  private Integer localEntityCounter = 0;

  protected AggregateRoot() {
    this.updatedAt = Instant.now();
  }

  public String getStrId() {
    return TSID.from(id).toString();
  }

  protected void touch() {
    this.updatedAt = Instant.now();
  }

  protected Integer nextLocalId() {
    return localEntityCounter++;
  }
}
