package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import com.rsargsyan.sprite.main_ctx.core.domain.entity.LocalEntity;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.Name;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.NameConverter;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Entity
@Table(name="user_profile")
public class UserProfile extends AccountScopedAggregateRoot {

  @OneToMany(
      mappedBy = "user",
      cascade = CascadeType.ALL,
      orphanRemoval = true
  )
  @Getter
  private final List<ApiKey> apiKeys = new ArrayList<>();

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

  public ApiKey createApiKey(String description) {
    ApiKey apiKey = new ApiKey(this, description);
    apiKeys.add(apiKey);
    return apiKey;
  }

  @Entity
  @Table(name = "api_key")
  public class ApiKey extends LocalEntity {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Getter
    @Transient
    private String key;

    @Getter
    @Column(nullable = false, unique = true)
    private String hashedKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @Getter
    @Column(nullable = false)
    private UserProfile userProfile;

    @Getter
    private boolean disabled = false;

    @Column(name = "last_access_time")
    @Getter
    private Instant lastAccessTime;

    @Getter
    private String description;

    private static String generateApiKey() {
      byte[] randomBytes = new byte[32];
      secureRandom.nextBytes(randomBytes);
      return base64Encoder.encodeToString(randomBytes);
    }

    public void disable() {
      this.disabled = true;
    }

    public void enable() {
      this.disabled = false;
    }

    private String hash(String key) {
      return passwordEncoder.encode(key); //implement crypto safe hash
    }

    public boolean check(String key) {
      return passwordEncoder.matches(key, hashedKey);
    }

    @SuppressWarnings("unused")
    ApiKey() {}

    private ApiKey(UserProfile userProfile, String description) {
      this.key = generateApiKey();
      this.hashedKey = hash(key);
      this.userProfile = userProfile;
      this.description = description;
    }
  }
}
