package com.rsargsyan.sprite.main_ctx.core.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rsargsyan.sprite.main_ctx.core.exception.InvalidThumbnailConfigException;

public record AvifThumbnailConfig(int resolution, SpriteSize spriteSize, int quality, int interval, int speed, String folderName) implements ThumbnailConfig {
  private static final java.util.regex.Pattern FOLDER_NAME_PATTERN =
      java.util.regex.Pattern.compile("^[a-zA-Z0-9._-]{1,63}$");

  public AvifThumbnailConfig {
    if (resolution <= 0) throw new InvalidThumbnailConfigException("Resolution must be positive");
    if (spriteSize == null) throw new InvalidThumbnailConfigException("Sprite size is required");
    if (quality < 0 || quality > 100) throw new InvalidThumbnailConfigException("AVIF quality must be between 0 and 100");
    if (interval <= 0) throw new InvalidThumbnailConfigException("Interval must be positive");
    if (speed < 0 || speed > 8) throw new InvalidThumbnailConfigException("AVIF speed must be between 0 and 8");
    if (folderName == null || !FOLDER_NAME_PATTERN.matcher(folderName).matches())
      throw new InvalidThumbnailConfigException("Folder name must be 1-63 characters: letters, digits, hyphens, underscores, or periods");
  }

  @Override
  @JsonProperty
  public String format() {
    return "avif";
  }
}
