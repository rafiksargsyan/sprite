package com.rsargsyan.sprite.main_ctx.core.app.dto;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.EmbeddedJobSpec;
import lombok.Value;
import org.springframework.lang.Nullable;

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
  @Nullable JobFailureReason failureReason;

  public static ThumbnailsGenerationJobDTO from(ThumbnailsGenerationJob job, String downloadUrl) {
    JobFailureReason failureReason = job.getFailureReason() != null
        ? JobFailureReason.from(job.getFailureReason())
        : null;
    return new ThumbnailsGenerationJobDTO(
        job.getStrId(), job.getVideoURL(), externalStatus(job.getStatus()), job.getJobSpec(),
        job.getStreamIndex(), job.isPreview(), job.getCreatedAt(), job.getStartedAt(), job.getFinishedAt(),
        downloadUrl, failureReason
    );
  }

  private static String externalStatus(ThumbnailsGenerationJob.Status status) {
    return switch (status) {
      case QUEUED, RECEIVED -> "SUBMITTED";
      default -> status.name();
    };
  }
}
