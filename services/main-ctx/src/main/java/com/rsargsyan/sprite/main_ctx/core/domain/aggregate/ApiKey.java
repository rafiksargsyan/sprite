package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Entity
@Table(name = "api_key")
public class ApiKey extends AccountScopedAggregateRoot {
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
  @JoinColumn(nullable = false)
  private Principal principal;

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

  private ApiKey(Principal principal, String description) {
    this.key = generateApiKey();
    this.hashedKey = hash(key);
    this.description = description;
    this.principal = principal;
  }
}
