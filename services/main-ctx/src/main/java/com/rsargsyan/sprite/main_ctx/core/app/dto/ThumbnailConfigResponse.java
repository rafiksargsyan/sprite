package com.rsargsyan.sprite.main_ctx.core.app.dto;

public sealed interface ThumbnailConfigResponse permits JpgThumbnailConfigResponse, WebpThumbnailConfigResponse {
  String format();
  int resolution();
  SpriteSizeResponse spriteSize();
}
