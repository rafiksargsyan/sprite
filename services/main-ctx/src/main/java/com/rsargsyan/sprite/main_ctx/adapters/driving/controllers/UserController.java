package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import com.rsargsyan.sprite.main_ctx.core.app.UserService;
import com.rsargsyan.sprite.main_ctx.core.app.dto.ApiKeyCreationDTO;
import com.rsargsyan.sprite.main_ctx.core.app.dto.UserCreationDTO;
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

  @PostMapping
  public ResponseEntity<UserDTO> createUser(
      @RequestBody UserCreationDTO req
  ) {
    UserDTO user = userService.create(req.getAccountId(), req.getName());
    return new ResponseEntity<>(user, HttpStatus.CREATED);
  }

  @PostMapping("/{userId}/api-key")
  public ResponseEntity<UserDTO> createApiKey(
      @PathVariable String userId,
      @RequestBody ApiKeyCreationDTO req
  ) {
    UserDTO user = userService.createApiKey(userId);
    return new ResponseEntity<>(user, HttpStatus.CREATED);
  }
}
