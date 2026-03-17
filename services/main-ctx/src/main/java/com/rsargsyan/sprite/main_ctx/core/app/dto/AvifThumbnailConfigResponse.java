package com.rsargsyan.sprite.main_ctx.core.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AvifThumbnailConfigResponse(int resolution, SpriteSizeResponse spriteSize, int quality, int interval, int speed, String folderName)
    implements ThumbnailConfigResponse {

  @Override
  @JsonProperty
  public String format() {
    return "avif";
  }
}
