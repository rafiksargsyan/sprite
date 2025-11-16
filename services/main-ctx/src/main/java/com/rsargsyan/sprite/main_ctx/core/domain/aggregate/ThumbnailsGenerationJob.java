package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;

import java.net.MalformedURLException;
import java.net.URL;

@Entity
public class ThumbnailsGenerationJob extends AggregateRoot {
  @Column(name = "url")
  @Getter
  private URL videoURL;

  public ThumbnailsGenerationJob(String videoURL) {
    try {
      this.videoURL = new URL(videoURL);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}