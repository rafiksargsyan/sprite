package com.rsargsyan.sprite.main_ctx.core.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BlurhashThumbnailConfigResponse(int resolution, int interval, int componentsX, int componentsY, String folderName)
    implements ThumbnailConfigResponse {

  @Override
  @JsonProperty
  public String format() { return "blurhash"; }

  @Override
  public SpriteSizeResponse spriteSize() { return null; }
}
