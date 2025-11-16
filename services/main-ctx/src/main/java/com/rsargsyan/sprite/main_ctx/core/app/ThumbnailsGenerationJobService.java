package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.ThumbnailsGenerationJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ThumbnailsGenerationJobService {

  private final ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository;

  @Autowired
  public ThumbnailsGenerationJobService(ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository) {
    this.thumbnailsGenerationJobRepository = thumbnailsGenerationJobRepository;
  }

  public ThumbnailsGenerationJob create(String videoURL) {
    ThumbnailsGenerationJob job = new ThumbnailsGenerationJob(videoURL);
    thumbnailsGenerationJobRepository.save(job);
    return job;
  }
}
