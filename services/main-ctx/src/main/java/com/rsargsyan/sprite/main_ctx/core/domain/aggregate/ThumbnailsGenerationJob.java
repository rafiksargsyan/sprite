package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.EmbeddedJobSpec;
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

  private String failureReason;

  @SuppressWarnings("unused")
  ThumbnailsGenerationJob() {}

  public ThumbnailsGenerationJob(Account account, String videoURL, EmbeddedJobSpec jobSpec) {
    super(account);
    this.status = Status.SUBMITTED;
    this.jobSpec = jobSpec;
    try {
      this.videoURL = new URL(videoURL);
    } catch (MalformedURLException e) {
      throw new MalformedUrlException("'%s' is not a valid URL".formatted(videoURL));
    }
  }

  public void queue() {
    if (this.status != Status.SUBMITTED) {
      throw new RuntimeException("Must be in submitted state"); //TODO: Create custom exception
    }
    this.status = Status.QUEUED;
    touch();
  }

  public void run() {
    if (this.status != Status.QUEUED) {
      throw new RuntimeException("Must be in queued state");
    }
    this.status = Status.IN_PROGRESS;
    touch();
  }

  public void succeed() {
    if (this.status != Status.IN_PROGRESS) {
      throw new RuntimeException("Must be in in_progress state");
    }
    this.status = Status.SUCCESS;
    touch();
  }

  public void fail(String reason) {
    this.status = Status.FAILURE;
    this.failureReason = reason;
    touch();
  }

  public enum Status {
    SUBMITTED,
    QUEUED,
    IN_PROGRESS,
    SUCCESS,
    FAILURE
  }
}
