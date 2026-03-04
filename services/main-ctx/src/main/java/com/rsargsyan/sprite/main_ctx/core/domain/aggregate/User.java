package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="`user`")
public class User extends AccountScopedAggregateRoot {

  @OneToMany(
      mappedBy = "user",
      cascade = CascadeType.ALL,
      orphanRemoval = true
  )
  @Getter
  private List<ApiKey> apiKeys = new ArrayList<>();

  @Getter
  private String name;

  @SuppressWarnings("unused")
  public User() {

  }

  public User(Long accountId, String name) {
    super(accountId);
    this.name = name;
  }

  public void createApiKey() {
    apiKeys.add(new ApiKey(this));
  }

  @Entity
  @Table(name = "api_key")
  public class ApiKey extends com.rsargsyan.sprite.main_ctx.core.domain.entity.Entity {
    @Getter
    private String key;

    @ManyToOne(fetch = FetchType.LAZY)
    @Getter
    private User user;

    ApiKey() {}

    ApiKey(User user) {
      this.key = UUID.randomUUID().toString(); //TODO: validate
      this.user = user;
    }
  }
}
