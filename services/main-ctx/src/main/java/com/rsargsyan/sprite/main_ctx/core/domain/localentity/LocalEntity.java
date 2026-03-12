package com.rsargsyan.sprite.main_ctx.core.domain.localentity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class LocalEntity {
  @Id
  @GeneratedValue
  private Long id;

  @Column(nullable = false)
  @Getter
  private Integer localId;

  protected LocalEntity(int localId) {
    this.localId = localId;
  }
}
