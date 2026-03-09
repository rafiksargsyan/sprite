package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.core.app.dto.ApiKeyDTO;
import com.rsargsyan.sprite.main_ctx.core.app.dto.UserDTO;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.Account;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ApiKey;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.Principal;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.UserProfile;
import com.rsargsyan.sprite.main_ctx.core.exception.AuthorizationException;
import com.rsargsyan.sprite.main_ctx.core.exception.InvalidIdException;
import com.rsargsyan.sprite.main_ctx.core.exception.PrincipalAlreadyExistsException;
import com.rsargsyan.sprite.main_ctx.core.exception.ResourceNotFoundException;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.AccountRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.ApiKeyRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.PrincipalRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.UserProfileRepository;
import io.hypersistence.tsid.TSID;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

  private UserProfileRepository userProfileRepository;
  private AccountRepository accountRepository;
  private PrincipalRepository principalRepository;
  private ApiKeyRepository apiKeyRepository;

  @Autowired
  public UserService(UserProfileRepository userProfileRepository, AccountRepository accountRepository,
                     PrincipalRepository principalRepository, ApiKeyRepository apiKeyRepository) {
    this.userProfileRepository = userProfileRepository;
    this.accountRepository = accountRepository;
    this.principalRepository = principalRepository;
    this.apiKeyRepository = apiKeyRepository;
  }
  public UserDTO create(String accountId, String name) {

//    User user = new User(TSID.from(accountId).toLong(), name);
//    this.userRepository.save(user);
//    return UserDTO.from(user);
    return null;
  }

  public ApiKeyDTO createApiKey(String actingUserId, String userProfileId, String description) {
    if (!TSID.isValid(userProfileId)) throw new InvalidIdException();
    if (!userProfileId.equals(actingUserId)) throw new AuthorizationException();
    Optional<UserProfile> userOpt = userProfileRepository.findById(TSID.from(userProfileId).toLong());
    if (userOpt.isEmpty()) {
      throw new ResourceNotFoundException();
    }
    UserProfile userProfile = userOpt.get();
    String key = userProfile.createApiKey(description);
    this.userProfileRepository.save(userProfile);
    ApiKey apiKey = userProfile.getApiKeyByKey(key);
    return ApiKeyDTO.from(apiKey, key);
  }

  @Transactional
  public UserDTO signUpWithExternal(String externalId, String name) {
    var principalName = name;
    if (name == null || name.isBlank()) name = "Your full name here";
    List<Principal> principalList = principalRepository.findByExternalId(externalId);
    if (!principalList.isEmpty()) {
      throw new PrincipalAlreadyExistsException();
    }
    Principal principal = new Principal(externalId, principalName);
    Account account = new Account();
    UserProfile userProfile = new UserProfile(account, principal, name);
    principalRepository.save(principal);
    accountRepository.save(account);
    userProfileRepository.save(userProfile);
    return UserDTO.from(userProfile);
  }
}
