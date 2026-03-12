package com.rsargsyan.sprite.main_ctx.core.app.dto;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.UserProfile;

public record UserDTO(String id, String name) {
  public static UserDTO from(UserProfile userProfile) {
    return new UserDTO(userProfile.getStrId(), userProfile.getFullName().value());
  }
}
