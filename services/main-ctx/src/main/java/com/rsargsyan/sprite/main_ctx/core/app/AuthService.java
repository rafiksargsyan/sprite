package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.adapters.driving.controllers.UserContext;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ApiKey;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.Principal;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.UserProfile;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.ApiKeyRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.PrincipalRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.UserProfileRepository;
import io.hypersistence.tsid.TSID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AuthService {
  private final ApiKeyRepository apiKeyRepository;
  private final UserProfileRepository userProfileRepository;

  private final PrincipalRepository principalRepository;

  @Autowired
  public AuthService(ApiKeyRepository apiKeyRepository,
                     UserProfileRepository userProfileRepository,
                     PrincipalRepository principalRepository) {
    this.apiKeyRepository = apiKeyRepository;
    this.userProfileRepository = userProfileRepository;
    this.principalRepository = principalRepository;
  }

  @Transactional
  public UserContext getUserContextByApiKey(String apiKeyId) {
    if (!TSID.isValid(apiKeyId)) return null;
    var apiKeyFromDBOpt = apiKeyRepository.findById(TSID.from(apiKeyId).toLong());
    if (apiKeyFromDBOpt.isEmpty()) return null;
    ApiKey apiKeyFromDB =  apiKeyFromDBOpt.get();
    UserProfile userProfile = apiKeyFromDB.getUserProfile();
    apiKeyFromDB.accessed();
    apiKeyRepository.save(apiKeyFromDB); //TODO: publish event and process in separate transaction
    return UserContext.builder().userProfileId(userProfile.getStrId())
        .accountId(userProfile.getAccount().getStrId())
        .externalId(userProfile.getPrincipal().getExternalId()).build();
  }

  @Transactional(readOnly = true)
  public String getUserProfileId(String externalId, String accountId) {
    List<Principal> principals = principalRepository.findByExternalId(externalId);
    if (principals.isEmpty()) return null;
    var principal = principals.get(0);
    if (!TSID.isValid(accountId)) return null;
    List<UserProfile> userProfiles = userProfileRepository.findByPrincipalIdAndAccountId(principal.getId(),
        TSID.from(accountId).toLong());
    if (userProfiles.isEmpty()) return null;
    return userProfiles.get(0).getStrId();
  }

  public boolean validateApiKey(String apiKeyId, String apiKey) {
    if (!TSID.isValid(apiKeyId)) return false;
    Optional<ApiKey> apiKeyFromDB = apiKeyRepository.findById(TSID.from(apiKeyId).toLong());
    if (apiKeyFromDB.isPresent() && apiKeyFromDB.get().check(apiKey)) {
      return true;
    }
    return false;
  }
}
