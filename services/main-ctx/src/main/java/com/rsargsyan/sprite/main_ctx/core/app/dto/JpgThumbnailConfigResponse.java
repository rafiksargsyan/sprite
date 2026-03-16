package com.rsargsyan.sprite.main_ctx.core.app.dto;

public record JpgThumbnailConfigResponse(int resolution, SpriteSizeResponse spriteSize, int quality, int interval)
    implements ThumbnailConfigResponse {

  @Override
  public String format() {
    return "jpg";
  }
}
