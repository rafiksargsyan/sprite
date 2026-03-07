package com.rsargsyan.sprite.main_ctx.core.app.dto;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.User;
import lombok.Value;

@Value
public class UserDTO {
  String id;
  String name;

  public static UserDTO from(User user) {
    return new UserDTO(user.getStrId(), user.getName().getValue());
  }
}
