package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ApiKey;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.UserProfile;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.ApiKeyRepository;
import io.hypersistence.tsid.TSID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final ApiKeyRepository apiKeyRepository;

  @Autowired
  public AuthService(ApiKeyRepository apiKeyRepository) {
    this.apiKeyRepository = apiKeyRepository;
  }

  @Transactional(readOnly = true)
  public UserContext getUserContextByApiKey(String apiKeyId) {
    var apiKeyFromDBOpt = apiKeyRepository.findById(TSID.from(apiKeyId).toLong());
    ApiKey apiKeyFromDB =  apiKeyFromDBOpt.get();
    UserProfile userProfile = apiKeyFromDB.getUserProfile();
    return UserContext.builder().userProfileId(userProfile.getStrId())
        .accountId(userProfile.getAccount().getStrId())
        .externalId(userProfile.getPrincipal().getExternalId()).build();
  }
}
