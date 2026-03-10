package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.FullName;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.NameConverter;
import com.rsargsyan.sprite.main_ctx.core.exception.ApiKeyLimitReachedException;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="user_profile")
public class UserProfile extends AccountScopedAggregateRoot {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "principal_id", nullable = false)
  private Principal principal;

  @Getter
  @Column(nullable = false, length = FullName.MAX_LENGTH)
  @Convert(converter = NameConverter.class)
  private FullName fullName;

  @OneToMany(
      mappedBy = "userProfile",
      cascade = CascadeType.ALL,
      orphanRemoval = true
  )
  private List<ApiKey> apiKeys = new ArrayList<>();

  @SuppressWarnings("unused")
  public UserProfile() {}

  public UserProfile(Account account, Principal principal, String name) {
    super(account);
    this.principal = principal;
    this.fullName = new FullName(name);
  }

  public String createApiKey(String description) {
    var apiKey = new ApiKey(this, description);
    if (apiKeys.size() >= 2) throw new ApiKeyLimitReachedException();
    apiKeys.add(apiKey);
    touch();
    return apiKey.getKey();
  }

  public ApiKey getApiKeyByKey(String key) {
    for (ApiKey apiKey : apiKeys) {
      if (apiKey.check(key)) return apiKey;
    }
    return null;
  }
}
