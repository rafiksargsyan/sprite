package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import com.rsargsyan.sprite.main_ctx.core.app.UserService;
import com.rsargsyan.sprite.main_ctx.core.app.dto.ApiKeyCreationDTO;
import com.rsargsyan.sprite.main_ctx.core.app.dto.ApiKeyDTO;
import com.rsargsyan.sprite.main_ctx.core.app.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
  private final UserService userService;

  @Autowired
  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/{userId}/api-key")
  public ResponseEntity<List<ApiKeyDTO>> listApiKeys(@PathVariable String userId) {
    String actingUserProfileId = UserContextHolder.get().getUserProfileId();
    return ResponseEntity.ok(userService.listApiKeys(actingUserProfileId, userId));
  }

  @PostMapping("/{userId}/api-key")
  public ResponseEntity<ApiKeyDTO> createApiKey(@PathVariable String userId,
                                                @RequestBody ApiKeyCreationDTO req) {
    String actingUserProfileId = UserContextHolder.get().getUserProfileId();
    ApiKeyDTO apiKeyDTO = userService.createApiKey(actingUserProfileId, userId, req.getDescription());
    return new ResponseEntity<>(apiKeyDTO, HttpStatus.CREATED);
  }

  @PutMapping("/{userId}/api-key/{keyId}/disable")
  public ResponseEntity<ApiKeyDTO> disableApiKey(@PathVariable String userId,
                                                 @PathVariable String keyId) {
    String actingUserProfileId = UserContextHolder.get().getUserProfileId();
    return ResponseEntity.ok(userService.disableApiKey(actingUserProfileId, userId, keyId));
  }

  @PutMapping("/{userId}/api-key/{keyId}/enable")
  public ResponseEntity<ApiKeyDTO> enableApiKey(@PathVariable String userId,
                                                @PathVariable String keyId) {
    String actingUserProfileId = UserContextHolder.get().getUserProfileId();
    return ResponseEntity.ok(userService.enableApiKey(actingUserProfileId, userId, keyId));
  }

  @DeleteMapping("/{userId}/api-key/{keyId}")
  public ResponseEntity<Void> deleteApiKey(@PathVariable String userId,
                                           @PathVariable String keyId) {
    String actingUserProfileId = UserContextHolder.get().getUserProfileId();
    userService.deleteApiKey(actingUserProfileId, userId, keyId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/signup-external")
  public ResponseEntity<UserDTO> signupExternal() {
    UserContext userContext = UserContextHolder.get();
    UserDTO user = userService.signUpWithExternal(userContext.getExternalId(), userContext.getFullName());
    return ResponseEntity.ok(user);
  }
}
