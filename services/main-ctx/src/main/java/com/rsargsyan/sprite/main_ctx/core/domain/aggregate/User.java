package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import com.rsargsyan.sprite.main_ctx.core.domain.entity.LocalEntity;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.Name;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.NameConverter;
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "principal_id", nullable = false)
  private Principal principal;

  @Getter
  @Column(nullable = false)
  @Convert(converter = NameConverter.class)
  private Name name;

  @SuppressWarnings("unused")
  public User() {

  }

  public User(Account account, Principal principal, String name) {
    super(account);
    this.principal = principal;
    this.name = new Name(name);
  }

  public void createApiKey() {
    apiKeys.add(new ApiKey(this));
  }

  @Entity
  @Table(name = "api_key")
  public class ApiKey extends LocalEntity {
    @Getter
    private String key;

    @ManyToOne(fetch = FetchType.LAZY)
    @Getter
    private User user;

    @SuppressWarnings("unused")
    ApiKey() {}

    ApiKey(User user) {
      this.key = UUID.randomUUID().toString(); //TODO: validate
      this.user = user;
    }
  }
}
