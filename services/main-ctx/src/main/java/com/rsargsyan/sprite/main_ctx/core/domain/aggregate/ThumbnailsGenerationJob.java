package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.ConfigProcessingStats;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.EmbeddedJobSpec;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.FailureReason;
import com.rsargsyan.sprite.main_ctx.core.exception.IllegalJobStateTransitionException;
import com.rsargsyan.sprite.main_ctx.core.exception.InvalidThumbnailConfigException;
import com.rsargsyan.sprite.main_ctx.core.exception.MalformedUrlException;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import org.hibernate.annotations.Type;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.List;

@Entity
@Getter
public class ThumbnailsGenerationJob extends AccountScopedAggregateRoot {
  @Column(name = "url")
  private URL videoURL;

  @Enumerated(EnumType.STRING)
  private Status status;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb", name = "job_spec")
  private EmbeddedJobSpec jobSpec;

  private Integer streamIndex;

  private boolean preview;

  @Enumerated(EnumType.STRING)
  private FailureReason failureReason;

  private Instant startedAt;

  private Instant finishedAt;

  private Instant lastHeartbeatAt;

  private int retryCount;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private List<ConfigProcessingStats> processingStats;

  private Double extractionCost;

  @SuppressWarnings("unused")
  ThumbnailsGenerationJob() {}

  public ThumbnailsGenerationJob(Account account, String videoURL, EmbeddedJobSpec jobSpec, Integer streamIndex, boolean preview) {
    super(account);
    if (streamIndex != null && streamIndex < 0)
      throw new InvalidThumbnailConfigException("Stream index must be a non-negative integer");
    this.status = Status.SUBMITTED;
    this.jobSpec = jobSpec;
    this.streamIndex = streamIndex;
    this.preview = preview;
    try {
      this.videoURL = new URL(videoURL);
    } catch (MalformedURLException e) {
      throw new MalformedUrlException("'%s' is not a valid URL".formatted(videoURL));
    }
  }

  public void queue() {
    if (this.status != Status.SUBMITTED && this.status != Status.RETRYING) {
      throw new IllegalJobStateTransitionException(this.status, Status.QUEUED);
    }
    this.status = Status.QUEUED;
    touch();
  }

  public void receive() {
    if (this.status != Status.QUEUED) {
      throw new IllegalJobStateTransitionException(this.status, Status.RECEIVED);
    }
    this.status = Status.RECEIVED;
    touch();
  }

  public void run() {
    if (this.status != Status.RECEIVED) {
      throw new IllegalJobStateTransitionException(this.status, Status.IN_PROGRESS);
    }
    this.status = Status.IN_PROGRESS;
    this.startedAt = Instant.now();
    touch();
  }

  public void retry() {
    this.status = Status.RETRYING;
    this.startedAt = null;
    this.lastHeartbeatAt = null;
    this.retryCount++;
    touch();
  }

  public void heartbeat() {
    this.lastHeartbeatAt = Instant.now();
  }

  public void recordStats(List<ConfigProcessingStats> stats) {
    this.processingStats = stats;
  }

  public void recordExtractionCost(double cost) {
    this.extractionCost = cost;
  }

  public void succeed() {
    if (this.status != Status.IN_PROGRESS) {
      throw new IllegalJobStateTransitionException(this.status, Status.SUCCESS);
    }
    this.status = Status.SUCCESS;
    this.finishedAt = Instant.now();
    touch();
  }

  public void fail(FailureReason reason) {
    this.status = Status.FAILURE;
    this.failureReason = reason;
    this.finishedAt = Instant.now();
    touch();
  }

  public enum Status {
    SUBMITTED,
    QUEUED,
    RECEIVED,
    IN_PROGRESS,
    RETRYING,
    SUCCESS,
    FAILURE
  }
}
