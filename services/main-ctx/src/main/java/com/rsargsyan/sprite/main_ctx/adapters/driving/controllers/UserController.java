package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import com.rsargsyan.sprite.main_ctx.core.app.UserService;
import com.rsargsyan.sprite.main_ctx.core.app.dto.ApiKeyCreationDTO;
import com.rsargsyan.sprite.main_ctx.core.app.dto.ApiKeyDTO;
import com.rsargsyan.sprite.main_ctx.core.app.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
  private final UserService userService;

  @Autowired
  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/{userId}/api-key")
  public ResponseEntity<ApiKeyDTO> createApiKey(@PathVariable String userId,
                                                @RequestBody ApiKeyCreationDTO req) {
    UserContext userContext = UserContextHolder.get();
    String actingUserProfileId = userContext.getUserProfileId();
    ApiKeyDTO apiKeyDTO = userService.createApiKey(actingUserProfileId, userId, req.getDescription());
    return new ResponseEntity<>(apiKeyDTO, HttpStatus.CREATED);
  }

  @PostMapping("/signup-external")
  public ResponseEntity<UserDTO> signupExternal() {
    UserContext userContext = UserContextHolder.get();
    UserDTO user = userService.signUpWithExternal(userContext.getExternalId(), userContext.getFullName());
    return ResponseEntity.ok(user);
  }
}
