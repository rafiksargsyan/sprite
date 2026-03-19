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
  boolean previewAvailable;
  Instant createdAt;
  Instant startedAt;
  Instant finishedAt;
  String downloadUrl;
  @Nullable JobFailureReason failureReason;

  public static ThumbnailsGenerationJobDTO from(ThumbnailsGenerationJob job, String downloadUrl, boolean previewAvailable) {
    JobFailureReason failureReason = job.getFailureReason() != null
        ? JobFailureReason.from(job.getFailureReason())
        : null;
    return new ThumbnailsGenerationJobDTO(
        job.getStrId(), job.getVideoURL(), externalStatus(job), job.getJobSpec(),
        job.getStreamIndex(), job.isPreview(), previewAvailable, job.getCreatedAt(), job.getStartedAt(), job.getFinishedAt(),
        downloadUrl, failureReason
    );
  }

  private static String externalStatus(ThumbnailsGenerationJob job) {
    return switch (job.getStatus()) {
      case QUEUED, RECEIVED -> job.getRetryCount() > 0 ? "IN_PROGRESS" : "SUBMITTED";
      case RETRYING -> "IN_PROGRESS";
      default -> job.getStatus().name();
    };
  }
}
