package com.rsargsyan.sprite.main_ctx.core.app.dto;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.EmbeddedJobSpec;
import lombok.Value;

import java.net.URL;
import java.time.Instant;

@Value
public class ThumbnailsGenerationJobDTO {
  String id;
  URL videoUrl;
  String status;
  EmbeddedJobSpec jobSpec;
  Integer streamIndex;
  boolean preview;
  Instant createdAt;
  Instant startedAt;
  Instant finishedAt;
  String downloadUrl;

  public static ThumbnailsGenerationJobDTO from(ThumbnailsGenerationJob job, String downloadUrl) {
    return new ThumbnailsGenerationJobDTO(
        job.getStrId(), job.getVideoURL(), externalStatus(job.getStatus()), job.getJobSpec(),
        job.getStreamIndex(), job.isPreview(), job.getCreatedAt(), job.getStartedAt(), job.getFinishedAt(), downloadUrl
    );
  }

  private static String externalStatus(ThumbnailsGenerationJob.Status status) {
    return status == ThumbnailsGenerationJob.Status.QUEUED ? "SUBMITTED" : status.name();
  }
}
