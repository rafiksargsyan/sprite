package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@MappedSuperclass
public abstract class AccountScopedAggregateRoot extends AggregateRoot {
  @Getter
  @Column(name = "account_id", nullable = false)
  private Long accountId;

  protected AccountScopedAggregateRoot() {

  }

  protected AccountScopedAggregateRoot(Long accountId) {
    this.accountId = accountId;
  }
}
