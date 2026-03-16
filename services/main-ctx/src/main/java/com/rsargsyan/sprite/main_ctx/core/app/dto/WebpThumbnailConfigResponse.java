package com.rsargsyan.sprite.main_ctx.core.app.dto;

public record WebpThumbnailConfigResponse(int resolution, SpriteSizeResponse spriteSize, int quality, int method, boolean lossless, int interval)
    implements ThumbnailConfigResponse {

  @Override
  public String format() {
    return "webp";
  }
}
