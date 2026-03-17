package com.rsargsyan.sprite.main_ctx.core.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WebpThumbnailConfigResponse(int resolution, SpriteSizeResponse spriteSize, int quality, int method, boolean lossless, int interval, String preset, String folderName)
    implements ThumbnailConfigResponse {

  @Override
  @JsonProperty
  public String format() {
    return "webp";
  }
}
