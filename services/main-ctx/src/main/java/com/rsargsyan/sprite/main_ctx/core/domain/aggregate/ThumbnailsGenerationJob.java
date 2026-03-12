package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import com.rsargsyan.sprite.main_ctx.core.exception.MalformedUrlException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;

import java.net.MalformedURLException;
import java.net.URL;

@Entity
@Getter
public class ThumbnailsGenerationJob extends AccountScopedAggregateRoot {
  @Column(name = "url")
  private URL videoURL;

  @Enumerated(EnumType.STRING)
  private Status status;

  @SuppressWarnings("unused")
  ThumbnailsGenerationJob() {}

  public ThumbnailsGenerationJob(Account account, String videoURL) {
    super(account);
    this.status = Status.SUBMITTED;
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


  public enum Status {
    SUBMITTED,
    QUEUED,
    IN_PROGRESS,
    SUCCESS,
    FAILURE
  }
}
