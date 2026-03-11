package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import com.rsargsyan.sprite.main_ctx.core.app.UserService;
import com.rsargsyan.sprite.main_ctx.core.app.dto.ApiKeyCreationDTO;
import com.rsargsyan.sprite.main_ctx.core.app.dto.ApiKeyDTO;
import com.rsargsyan.sprite.main_ctx.core.app.dto.UserCreationDTO;
import com.rsargsyan.sprite.main_ctx.core.app.dto.UserDTO;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.Principal;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.UserProfile;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.PrincipalRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.UserProfileRepository;
import io.hypersistence.tsid.TSID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
  private final UserService userService;
  private final PrincipalRepository principalRepository;
  private final UserProfileRepository userProfileRepository;

  @Autowired
  public UserController(UserService userService, PrincipalRepository principalRepository,
                        UserProfileRepository userProfileRepository) {
    this.userService = userService;
    this.principalRepository = principalRepository;
    this.userProfileRepository = userProfileRepository;
  }

  @PostMapping
  public ResponseEntity<UserDTO> createUser(@RequestBody UserCreationDTO req) {
    UserDTO user = userService.create(req.getAccountId(), req.getName());
    return new ResponseEntity<>(user, HttpStatus.CREATED);
  }

  @PostMapping("/{userId}/api-key")
  public ResponseEntity<ApiKeyDTO> createApiKey(@PathVariable String userId,
                                                @RequestBody ApiKeyCreationDTO req) {
    UserContext userContext = UserContextHolder.get();
    String userProfileId = userContext.getUserProfileId();
    if (userProfileId == null) {
      UserProfile userProfile = findUserProfileByExternalIdAndAccountId(userContext.getExternalId(),
          userContext.getAccountId());
      userProfileId = userProfile != null ? userProfile.getStrId() : null;
    }
    ApiKeyDTO apiKeyDTO = userService.createApiKey(userProfileId, userId, req.getDescription());
    return new ResponseEntity<>(apiKeyDTO, HttpStatus.CREATED);
  }

  @PostMapping("/signup-external")
  public ResponseEntity<UserDTO> signupExternal() {
    UserContext userContext = UserContextHolder.get();
    UserDTO user = userService.signUpWithExternal(userContext.getExternalId(), userContext.getFullName());
    return new ResponseEntity<>(user, HttpStatus.CREATED);
  }

  private UserProfile findUserProfileByExternalIdAndAccountId(String externalId, String accountId) {
    if (!TSID.isValid(accountId)) return null;
    List<Principal> principalList = principalRepository.findByExternalId(externalId);
    if (principalList.isEmpty()) return null;
    var principal = principalList.get(0);
    List<UserProfile> userProfiles = userProfileRepository.findByPrincipalIdAndAccountId(principal.getId(),
        TSID.from(accountId).toLong());
    if (userProfiles.isEmpty()) return null;
    return userProfiles.get(0);
  }
}
