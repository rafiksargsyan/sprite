package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserContext {
  String externalId;
  String id;
  String accountId;
}

