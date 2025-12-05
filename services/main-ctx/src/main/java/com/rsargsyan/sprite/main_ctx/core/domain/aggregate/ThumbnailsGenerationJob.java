package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;

import java.net.MalformedURLException;
import java.net.URL;

@Entity
public class ThumbnailsGenerationJob extends AggregateRoot {
  @Column(name = "url")
  @Getter
  private URL videoURL;

  @Getter
  @Enumerated(EnumType.STRING)
  private Status status;

  @SuppressWarnings("unused")
  public ThumbnailsGenerationJob() {}

  public ThumbnailsGenerationJob(String videoURL) {
    this.status = Status.SUBMITTED;
    try {
      this.videoURL = new URL(videoURL);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
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


  public enum Status {
    SUBMITTED,
    QUEUED,
    IN_PROGRESS,
    SUCCESS,
    FAILURE
  }
}
