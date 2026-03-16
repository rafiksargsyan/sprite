package com.rsargsyan.sprite.main_ctx.core.domain.valueobject;

import com.rsargsyan.sprite.main_ctx.core.exception.InvalidThumbnailConfigException;

public record WebpThumbnailConfig(int resolution, SpriteSize spriteSize, int quality, int method, boolean lossless, int interval) implements ThumbnailConfig {
  public WebpThumbnailConfig {
    if (resolution <= 0) throw new InvalidThumbnailConfigException("Resolution must be positive");
    if (spriteSize == null) throw new InvalidThumbnailConfigException("Sprite size is required");
    if (quality < 0 || quality > 100) throw new InvalidThumbnailConfigException("WebP quality must be between 0 and 100");
    if (method < 0 || method > 6) throw new InvalidThumbnailConfigException("WebP method must be between 0 and 6");
    if (interval <= 0) throw new InvalidThumbnailConfigException("Interval must be positive");
  }

  @Override
  public String format() {
    return "webp";
  }

  @Override
  public String subfolderName() {
    return "webp_r%d_q%d_m%d_%s_i%d_%dx%d".formatted(
        resolution, quality, method, lossless ? "lossless" : "lossy", interval,
        spriteSize.rows(), spriteSize.cols()
    );
  }
}
