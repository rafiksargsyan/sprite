package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.core.app.dto.ThumbnailsGenerationJobDTO;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.ThumbnailsGenerationJobRepository;
import io.hypersistence.tsid.TSID;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ThumbnailsGenerationJobService {

  private ApplicationEventPublisher applicationEventPublisher;
  private final ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository;

  @Autowired
  public ThumbnailsGenerationJobService(
      ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository,
      ApplicationEventPublisher applicationEventPublisher
  ) {
    this.thumbnailsGenerationJobRepository = thumbnailsGenerationJobRepository;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Transactional
  public ThumbnailsGenerationJobDTO create(String videoURL) {
    ThumbnailsGenerationJob job = new ThumbnailsGenerationJob(videoURL);
    thumbnailsGenerationJobRepository.save(job);
    applicationEventPublisher.publishEvent(new ThumbnailsGenerationJobUpsertEvent(job.getId()));
    return ThumbnailsGenerationJobDTO.from(job);
  }

  @Transactional
  public ThumbnailsGenerationJobDTO touch(String accountId, String id) {
    Optional<ThumbnailsGenerationJob> job = thumbnailsGenerationJobRepository
        .findByAccountIdAndId(TSID.from(accountId).toLong(), TSID.from(id).toLong());
    if (job.isEmpty()) {
      throw new RuntimeException("TODO"); // create custom exception
    }
    job.get().touch();
    thumbnailsGenerationJobRepository.save(job.get());
    applicationEventPublisher.publishEvent(new ThumbnailsGenerationJobUpsertEvent(job.get().getId()));
    return ThumbnailsGenerationJobDTO.from(job.get());
  }

  @Transactional
  public void run(String id) {
    try {
      thumbnailsGenerationJobRepository.findById(TSID.from(id).toLong()).ifPresentOrElse(
          job -> {
            job.run();
            thumbnailsGenerationJobRepository.save(job);
            applicationEventPublisher.publishEvent(new ThumbnailsGenerationJobUpsertEvent(job.getId()));
          },
          () -> {
            throw new RuntimeException();
          }
      );
    } catch (Exception e) {
      //
    }
  }
}
