package com.rsargsyan.sprite.main_ctx.core.domain.valueobject;

import com.rsargsyan.sprite.main_ctx.core.exception.InvalidThumbnailConfigException;

public record JpgThumbnailConfig(int resolution, SpriteSize spriteSize, int quality, int interval) implements ThumbnailConfig {
  public JpgThumbnailConfig {
    if (resolution <= 0) throw new InvalidThumbnailConfigException("Resolution must be positive");
    if (spriteSize == null) throw new InvalidThumbnailConfigException("Sprite size is required");
    if (quality < 1 || quality > 100) throw new InvalidThumbnailConfigException("JPG quality must be between 1 and 100");
    if (interval <= 0) throw new InvalidThumbnailConfigException("Interval must be positive");
  }

  @Override
  public String format() {
    return "jpg";
  }

  @Override
  public String subfolderName() {
    return "jpg_r%d_q%d_i%d_%dx%d".formatted(resolution, quality, interval, spriteSize.rows(), spriteSize.cols());
  }
}
