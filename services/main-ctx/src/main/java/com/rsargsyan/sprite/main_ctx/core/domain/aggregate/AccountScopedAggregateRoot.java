package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import jakarta.persistence.*;
import lombok.Getter;

@MappedSuperclass
public abstract class AccountScopedAggregateRoot extends AggregateRoot {
  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  protected AccountScopedAggregateRoot() {
  }

  protected AccountScopedAggregateRoot(Account account) {
    this.account = account;
  }
}
