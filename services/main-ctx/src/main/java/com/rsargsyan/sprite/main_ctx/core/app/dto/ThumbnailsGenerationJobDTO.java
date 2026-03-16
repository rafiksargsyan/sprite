package com.rsargsyan.sprite.main_ctx.core.app.dto;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.EmbeddedJobSpec;
import lombok.Value;

import java.net.URL;

@Value
public class ThumbnailsGenerationJobDTO {
  String id;
  URL videoUrl;
  EmbeddedJobSpec jobSpec;

  public static ThumbnailsGenerationJobDTO from(ThumbnailsGenerationJob job) {
    return new ThumbnailsGenerationJobDTO(job.getStrId(), job.getVideoURL(), job.getJobSpec());
  }
}
