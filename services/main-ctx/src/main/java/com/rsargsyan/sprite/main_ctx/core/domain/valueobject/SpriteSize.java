package com.rsargsyan.sprite.main_ctx.core.domain.valueobject;

import com.rsargsyan.sprite.main_ctx.core.exception.InvalidThumbnailConfigException;

public record SpriteSize(int rows, int cols) {
  public SpriteSize {
    if (rows <= 0 || cols <= 0) {
      throw new InvalidThumbnailConfigException("Sprite size rows and cols must be positive");
    }
  }
}
