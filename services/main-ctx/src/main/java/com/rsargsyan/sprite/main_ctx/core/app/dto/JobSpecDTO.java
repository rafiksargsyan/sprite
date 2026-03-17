package com.rsargsyan.sprite.main_ctx.core.app.dto;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.JobSpec;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.AvifThumbnailConfig;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.JpgThumbnailConfig;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.ThumbnailConfig;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.WebpThumbnailConfig;
import lombok.Value;

import java.util.List;

@Value
public class JobSpecDTO {
  String id;
  String name;
  String description;
  List<ThumbnailConfigResponse> configs;

  public static JobSpecDTO from(JobSpec jobSpec) {
    return new JobSpecDTO(
        jobSpec.getStrId(),
        jobSpec.getName(),
        jobSpec.getDescription(),
        jobSpec.getConfigs().stream().map(JobSpecDTO::toResponse).toList()
    );
  }

  private static ThumbnailConfigResponse toResponse(ThumbnailConfig config) {
    if (config instanceof JpgThumbnailConfig c) {
      return new JpgThumbnailConfigResponse(
          c.resolution(), new SpriteSizeResponse(c.spriteSize().rows(), c.spriteSize().cols()), c.quality(), c.interval(), c.folderName()
      );
    } else if (config instanceof WebpThumbnailConfig c) {
      return new WebpThumbnailConfigResponse(
          c.resolution(), new SpriteSizeResponse(c.spriteSize().rows(), c.spriteSize().cols()), c.quality(), c.method(), c.lossless(), c.interval(), c.preset(), c.folderName()
      );
    } else if (config instanceof AvifThumbnailConfig c) {
      return new AvifThumbnailConfigResponse(
          c.resolution(), new SpriteSizeResponse(c.spriteSize().rows(), c.spriteSize().cols()), c.quality(), c.interval(), c.speed(), c.folderName()
      );
    }
    throw new IllegalStateException("Unknown config type: " + config.getClass());
  }
}
