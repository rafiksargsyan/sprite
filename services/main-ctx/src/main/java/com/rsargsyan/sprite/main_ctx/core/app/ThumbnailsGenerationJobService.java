package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.core.Util;
import com.rsargsyan.sprite.main_ctx.core.app.dto.ThumbnailsGenerationJobDTO;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;
import com.rsargsyan.sprite.main_ctx.core.exception.ResourceNotFoundException;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.AccountRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.ThumbnailsGenerationJobRepository;
import io.hypersistence.tsid.TSID;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ThumbnailsGenerationJobService {

  private final ApplicationEventPublisher applicationEventPublisher;
  private final ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository;

  private final AccountRepository accountRepository;

  @Autowired
  public ThumbnailsGenerationJobService(
      ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository,
      ApplicationEventPublisher applicationEventPublisher,
      AccountRepository accountRepository
  ) {
    this.thumbnailsGenerationJobRepository = thumbnailsGenerationJobRepository;
    this.applicationEventPublisher = applicationEventPublisher;
    this.accountRepository = accountRepository;
  }

  @Transactional
  public ThumbnailsGenerationJobDTO create(String accountId, String videoURL) {
    var accountOpt = accountRepository.findById(Util.validateTSID(accountId));
    if (accountOpt.isEmpty()) throw new ResourceNotFoundException();
    ThumbnailsGenerationJob job = new ThumbnailsGenerationJob(accountOpt.get(), videoURL);
    thumbnailsGenerationJobRepository.save(job);
    applicationEventPublisher.publishEvent(new ThumbnailsGenerationJobUpsertEvent(job.getId()));
    return ThumbnailsGenerationJobDTO.from(job);
  }

  @Transactional
  public void run(String id) {
    try {
      thumbnailsGenerationJobRepository.findById(Util.validateTSID(id)).ifPresentOrElse(
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
