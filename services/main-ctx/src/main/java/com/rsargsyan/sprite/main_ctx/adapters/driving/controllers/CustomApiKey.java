package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;

public class CustomApiKey extends AbstractAuthenticationToken {
  @Getter
  private String apiKey;

  public CustomApiKey(String apiKey) {
    super(null);
    this.apiKey = apiKey;
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
