package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;

public class CustomApiKey extends AbstractAuthenticationToken {
  @Getter
  private String apiKey;

  @Getter
  private String apiKeyId;

  public CustomApiKey(String apiKeyId, String apiKey) {
    super(null);
    this.apiKey = apiKey;
    this.apiKeyId = apiKeyId;
  }

  @Override
  public Object getCredentials() {
    return this.apiKey;
  }

  @Override
  public Object getPrincipal() {
    return this.apiKey;
  }
}
