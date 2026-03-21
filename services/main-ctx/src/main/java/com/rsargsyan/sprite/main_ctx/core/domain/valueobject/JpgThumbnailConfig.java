package com.rsargsyan.sprite.main_ctx.core.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rsargsyan.sprite.main_ctx.core.exception.InvalidThumbnailConfigException;

public record JpgThumbnailConfig(int resolution, SpriteSize spriteSize, int quality, int interval,
                                 String folderName) implements ThumbnailConfig {
  private static final java.util.regex.Pattern FOLDER_NAME_PATTERN =
      java.util.regex.Pattern.compile("^[a-zA-Z0-9._-]{1,63}$");

  public JpgThumbnailConfig {
    if (resolution <= 0) throw new InvalidThumbnailConfigException("Resolution must be positive");
    if (spriteSize == null) throw new InvalidThumbnailConfigException("Sprite size is required");
    if (quality < 1 || quality > 100) throw new InvalidThumbnailConfigException("JPG quality must be between 1 and 100");
    if (interval <= 0) throw new InvalidThumbnailConfigException("Interval must be positive");
    if (folderName == null || !FOLDER_NAME_PATTERN.matcher(folderName).matches())
      throw new InvalidThumbnailConfigException("Folder name must be 1-63 characters: letters, digits, hyphens, underscores, or periods");
  }

  @Override
  @JsonProperty
  public String format() {
    return "jpg";
  }

  @Override
  public double postProcessingCost(int thumbnailCount, int resolution) {
    double countFactor = thumbnailCount / 600.0;
    double resFactor = 0.937 + 0.063 * Math.pow(resolution / 120.0, 2);
    double spriteFactor = 25.0 / (spriteSize().rows() * spriteSize().cols());
    return 105 * countFactor * resFactor * spriteFactor;
  }
}
