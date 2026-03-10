package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ApiKey;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.ApiKeyRepository;
import io.hypersistence.tsid.TSID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CustomApiKeyAuthenticationProvider implements AuthenticationProvider {
  @Autowired
  private ApiKeyRepository apiKeyRepository;

  @Override
  public Authentication authenticate(Authentication auth) throws AuthenticationException {
    CustomApiKey apiKey = (CustomApiKey) auth;
    Optional<ApiKey> apiKeyFromDB = apiKeyRepository.findById(TSID.from(apiKey.getApiKeyId()).toLong());
    if (apiKeyFromDB.isPresent() && apiKeyFromDB.get().check(apiKey.getApiKey())) {
      apiKey.setAuthenticated(true);
    }
    return apiKey;
  }

  @Override
  public boolean supports(Class<?> auth) {
    return CustomApiKey.class.isAssignableFrom(auth);
  }
}
