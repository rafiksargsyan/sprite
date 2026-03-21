package com.rsargsyan.sprite.main_ctx.core.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rsargsyan.sprite.main_ctx.core.exception.InvalidThumbnailConfigException;

public record BlurhashThumbnailConfig(int interval, int componentsX,
                                      int componentsY, String folderName) implements ThumbnailConfig {
  private static final java.util.regex.Pattern FOLDER_NAME_PATTERN =
      java.util.regex.Pattern.compile("^[a-zA-Z0-9._-]{1,63}$");

  public BlurhashThumbnailConfig {
    if (interval <= 0) throw new InvalidThumbnailConfigException("Interval must be positive");
    if (componentsX < 1 || componentsX > 9) throw new InvalidThumbnailConfigException("componentsX must be between 1 and 9");
    if (componentsY < 1 || componentsY > 9) throw new InvalidThumbnailConfigException("componentsY must be between 1 and 9");
    if (folderName == null || !FOLDER_NAME_PATTERN.matcher(folderName).matches())
      throw new InvalidThumbnailConfigException("Folder name must be 1-63 characters: letters, digits, hyphens, underscores, or periods");
  }

  @Override
  @JsonProperty
  public String format() { return "blurhash"; }

  @Override
  public int resolution() { return 32; }

  @Override
  public SpriteSize spriteSize() { return null; }

  @Override
  public int quality() { return 0; }

  @Override
  public double postProcessingCost(int thumbnailCount, int resolution) {
    return 65.0 * (thumbnailCount / 600.0) * (componentsX * componentsY) / 12.0;
  }
}
