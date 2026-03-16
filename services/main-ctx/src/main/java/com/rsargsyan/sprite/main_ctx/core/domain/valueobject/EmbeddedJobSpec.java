package com.rsargsyan.sprite.main_ctx.core.domain.valueobject;

import com.rsargsyan.sprite.main_ctx.core.exception.InvalidThumbnailConfigException;

import java.util.List;

public record EmbeddedJobSpec(List<ThumbnailConfig> configs) {
  public EmbeddedJobSpec {
    if (configs == null || configs.isEmpty()) throw new InvalidThumbnailConfigException("At least one config is required");
    configs = List.copyOf(configs);
  }
}
