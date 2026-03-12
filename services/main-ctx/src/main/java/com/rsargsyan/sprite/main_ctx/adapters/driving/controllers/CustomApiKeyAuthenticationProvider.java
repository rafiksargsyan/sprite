package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import com.rsargsyan.sprite.main_ctx.core.app.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class CustomApiKeyAuthenticationProvider implements AuthenticationProvider {
  private final AuthService authService;

  @Autowired
  public CustomApiKeyAuthenticationProvider(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public Authentication authenticate(Authentication auth) throws AuthenticationException {
    CustomApiKey apiKey = (CustomApiKey) auth;
    if (authService.validateApiKey(apiKey.getApiKeyId(), apiKey.getApiKey())) {
      apiKey.setAuthenticated(true);
    }
    return apiKey;
  }

  @Override
  public boolean supports(Class<?> auth) {
    return CustomApiKey.class.isAssignableFrom(auth);
  }
}
