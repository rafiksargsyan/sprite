package com.rsargsyan.sprite.main_ctx.core.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JpgThumbnailConfigResponse(int resolution, SpriteSizeResponse spriteSize, int quality, int interval)
    implements ThumbnailConfigResponse {

  @Override
  @JsonProperty
  public String format() {
    return "jpg";
  }
}
