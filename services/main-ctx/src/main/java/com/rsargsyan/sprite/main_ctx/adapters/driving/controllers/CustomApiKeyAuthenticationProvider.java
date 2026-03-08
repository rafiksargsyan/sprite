package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class CustomApiKeyAuthenticationProvider implements AuthenticationProvider {
  @Override
  public Authentication authenticate(Authentication auth) throws AuthenticationException {
    CustomApiKey apiKey = (CustomApiKey) auth;
    apiKey.setAuthenticated(true);
    return apiKey;
  }

  @Override
  public boolean supports(Class<?> auth) {
    return CustomApiKey.class.isAssignableFrom(auth);
  }
}

