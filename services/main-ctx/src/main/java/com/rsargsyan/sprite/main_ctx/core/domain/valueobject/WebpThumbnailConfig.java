package com.rsargsyan.sprite.main_ctx.core.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rsargsyan.sprite.main_ctx.core.exception.InvalidThumbnailConfigException;

public record WebpThumbnailConfig(int resolution, SpriteSize spriteSize, int quality, int method, int interval, String folderName) implements ThumbnailConfig {
  private static final java.util.regex.Pattern FOLDER_NAME_PATTERN =
      java.util.regex.Pattern.compile("^[a-zA-Z0-9._-]{1,63}$");

  public WebpThumbnailConfig {
    if (resolution <= 0) throw new InvalidThumbnailConfigException("Resolution must be positive");
    if (spriteSize == null) throw new InvalidThumbnailConfigException("Sprite size is required");
    if (quality < 0 || quality > 100) throw new InvalidThumbnailConfigException("WebP quality must be between 0 and 100");
    if (method < 0 || method > 6) throw new InvalidThumbnailConfigException("WebP method must be between 0 and 6");
    if (interval <= 0) throw new InvalidThumbnailConfigException("Interval must be positive");
    if (folderName == null || !FOLDER_NAME_PATTERN.matcher(folderName).matches())
      throw new InvalidThumbnailConfigException("Folder name must be 1-63 characters: letters, digits, hyphens, underscores, or periods");
  }

  @Override
  @JsonProperty
  public String format() {
    return "webp";
  }

  private static final double[] METHOD_FACTORS = {0.31, 0.36, 0.48, 0.97, 1.00, 1.10, 1.58};

  @Override
  public double postProcessingCost(int thumbnailCount, int resolution) {
    double countFactor = thumbnailCount / 600.0;
    double resFactor = 0.705 + 0.295 * Math.pow(resolution / 120.0, 2);
    double spriteFactor = 25.0 / (spriteSize().rows() * spriteSize().cols());
    return 150 * countFactor * resFactor * spriteFactor * METHOD_FACTORS[method];
  }
}
