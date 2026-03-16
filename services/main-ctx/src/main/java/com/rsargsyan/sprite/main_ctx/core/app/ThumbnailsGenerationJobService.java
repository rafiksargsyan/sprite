package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.core.Util;
import com.rsargsyan.sprite.main_ctx.core.app.dto.ThumbnailsGenerationJobDTO;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;
import com.rsargsyan.sprite.main_ctx.core.exception.ResourceNotFoundException;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.AccountRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.JobSpecRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.ThumbnailsGenerationJobRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class ThumbnailsGenerationJobService {

  private final ApplicationEventPublisher applicationEventPublisher;
  private final ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository;
  private final AccountRepository accountRepository;
  private final JobSpecRepository jobSpecRepository;

  @Autowired
  public ThumbnailsGenerationJobService(
      ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository,
      ApplicationEventPublisher applicationEventPublisher,
      AccountRepository accountRepository,
      JobSpecRepository jobSpecRepository
  ) {
    this.thumbnailsGenerationJobRepository = thumbnailsGenerationJobRepository;
    this.applicationEventPublisher = applicationEventPublisher;
    this.accountRepository = accountRepository;
    this.jobSpecRepository = jobSpecRepository;
  }

  @Transactional
  public ThumbnailsGenerationJobDTO create(String accountId, String videoURL, String jobSpecId) {
    var accountLongId = Util.validateTSID(accountId);
    var account = accountRepository.findById(accountLongId)
        .orElseThrow(ResourceNotFoundException::new);
    var jobSpec = jobSpecRepository.findByAccountIdAndId(accountLongId, Util.validateTSID(jobSpecId))
        .orElseThrow(ResourceNotFoundException::new);
    var job = new ThumbnailsGenerationJob(account, videoURL, jobSpec.toEmbedded());
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
