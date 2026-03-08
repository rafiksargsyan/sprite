package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.Name;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.NameConverter;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name="user_profile")
public class UserProfile extends AccountScopedAggregateRoot {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "principal_id", nullable = false)
  private Principal principal;

  @Getter
  @Column(nullable = false)
  @Convert(converter = NameConverter.class)
  private Name name;

  @SuppressWarnings("unused")
  public UserProfile() {}

  public UserProfile(Account account, Principal principal, String name) {
    super(account);
    this.principal = principal;
    this.name = new Name(name);
  }
}
