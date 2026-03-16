package com.rsargsyan.sprite.main_ctx.core.domain.valueobject;

public sealed interface ThumbnailConfig permits JpgThumbnailConfig, WebpThumbnailConfig {
  String format();
  int resolution();
  SpriteSize spriteSize();
  int quality();
  int interval();
  String subfolderName();
}
