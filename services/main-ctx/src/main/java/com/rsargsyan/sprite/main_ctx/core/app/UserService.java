package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.core.Util;
import com.rsargsyan.sprite.main_ctx.core.app.dto.ApiKeyDTO;
import com.rsargsyan.sprite.main_ctx.core.app.dto.UserDTO;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.Account;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ApiKey;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.Principal;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.UserProfile;
import com.rsargsyan.sprite.main_ctx.core.exception.AuthorizationException;
import com.rsargsyan.sprite.main_ctx.core.exception.ResourceNotFoundException;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.AccountRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.ApiKeyRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.PrincipalRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.UserProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

  private UserProfileRepository userProfileRepository;
  private AccountRepository accountRepository;
  private PrincipalRepository principalRepository;

  @Autowired
  public UserService(UserProfileRepository userProfileRepository, AccountRepository accountRepository,
                     PrincipalRepository principalRepository, ApiKeyRepository apiKeyRepository) {
    this.userProfileRepository = userProfileRepository;
    this.accountRepository = accountRepository;
    this.principalRepository = principalRepository;
  }

  public ApiKeyDTO createApiKey(String actingUserId, String userProfileIdStr, String description) {
    Long userProfileId = Util.validateTSID(userProfileIdStr);
    if (!userProfileIdStr.equals(actingUserId)) throw new AuthorizationException();
    UserProfile userProfile = findUserProfileByIdOrThrow(userProfileId);
    String key = userProfile.createApiKey(description);
    this.userProfileRepository.save(userProfile);
    ApiKey apiKey = userProfile.getApiKeyByKey(key);
    return ApiKeyDTO.from(apiKey, key);
  }

  @Transactional
  public UserDTO signUpWithExternal(String externalId, String name) {
    List<Principal> principalList = principalRepository.findByExternalId(externalId);
    if (!principalList.isEmpty()) {
      var existing = userProfileRepository.findByPrincipalId(principalList.get(0).getId());
      if (existing.isEmpty()) throw new ResourceNotFoundException();
      return UserDTO.from(existing.get(0));
    }
    if (name == null || name.isBlank()) name = "Your full name here";
    Principal principal = new Principal(externalId, name);
    Account account = new Account();
    UserProfile userProfile = new UserProfile(account, principal, name);
    principalRepository.save(principal);
    accountRepository.save(account);
    userProfileRepository.save(userProfile);
    return UserDTO.from(userProfile);
  }

  private UserProfile findUserProfileByIdOrThrow(Long id) {
    Assert.notNull(id, "id can't be null");
    Optional<UserProfile> userOpt = userProfileRepository.findById(id);
    if (userOpt.isEmpty()) {
      throw new ResourceNotFoundException();
    }
    return userOpt.get();
  }
}
