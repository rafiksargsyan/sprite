package com.rsargsyan.sprite.main_ctx.core.app.dto;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ApiKey;

import java.time.Instant;

public record ApiKeyDTO(String id, String key, Instant lastAccessTime,
                        String description) {
  public static ApiKeyDTO from(ApiKey apiKey, String key) {
    return new ApiKeyDTO(apiKey.getStrId(), key,
        apiKey.getLastAccessTime(), apiKey.getDescription());
  }
}
