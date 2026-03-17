package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.Config;
import com.rsargsyan.sprite.main_ctx.core.Util;
import com.rsargsyan.sprite.main_ctx.core.app.dto.ThumbnailsGenerationJobCreationDTO;
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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class ThumbnailsGenerationJobService {

  private static final Duration DOWNLOAD_WINDOW = Duration.ofHours(2);

  private final ApplicationEventPublisher applicationEventPublisher;
  private final ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository;
  private final AccountRepository accountRepository;
  private final JobSpecRepository jobSpecRepository;
  private final S3Presigner s3Presigner;
  private final Config config;

  @Autowired
  public ThumbnailsGenerationJobService(
      ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository,
      ApplicationEventPublisher applicationEventPublisher,
      AccountRepository accountRepository,
      JobSpecRepository jobSpecRepository,
      S3Presigner s3Presigner,
      Config config
  ) {
    this.thumbnailsGenerationJobRepository = thumbnailsGenerationJobRepository;
    this.applicationEventPublisher = applicationEventPublisher;
    this.accountRepository = accountRepository;
    this.jobSpecRepository = jobSpecRepository;
    this.s3Presigner = s3Presigner;
    this.config = config;
  }

  public List<ThumbnailsGenerationJobDTO> findAll(String accountId) {
    return thumbnailsGenerationJobRepository.findByAccountId(Util.validateTSID(accountId))
        .stream().map(job -> ThumbnailsGenerationJobDTO.from(job, presignedDownloadUrl(job))).toList();
  }

  public ThumbnailsGenerationJobDTO findById(String accountId, String jobId) {
    var job = thumbnailsGenerationJobRepository
        .findByAccountIdAndId(Util.validateTSID(accountId), Util.validateTSID(jobId))
        .orElseThrow(ResourceNotFoundException::new);
    return ThumbnailsGenerationJobDTO.from(job, presignedDownloadUrl(job));
  }

  public long getMaxFileSizeBytes() {
    return config.maxVideoFileSizeBytes;
  }

  @Transactional
  public ThumbnailsGenerationJobDTO create(String accountId, ThumbnailsGenerationJobCreationDTO dto) {
    var accountLongId = Util.validateTSID(accountId);
    var account = accountRepository.findById(accountLongId)
        .orElseThrow(ResourceNotFoundException::new);
    var jobSpec = jobSpecRepository.findByAccountIdAndId(accountLongId, Util.validateTSID(dto.getJobSpecId()))
        .orElseThrow(ResourceNotFoundException::new);
    var job = new ThumbnailsGenerationJob(account, dto.getVideoURL(), jobSpec.toEmbedded(), dto.getStreamIndex(), dto.isPreview());
    thumbnailsGenerationJobRepository.save(job);
    applicationEventPublisher.publishEvent(new ThumbnailsGenerationJobUpsertEvent(job.getId()));
    return ThumbnailsGenerationJobDTO.from(job, null);
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

  private String presignedDownloadUrl(ThumbnailsGenerationJob job) {
    if (job.getFinishedAt() == null) return null;
    Instant expiry = job.getFinishedAt().plus(DOWNLOAD_WINDOW);
    Instant now = Instant.now();
    if (!now.isBefore(expiry)) return null;
    var presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.between(now, expiry))
        .getObjectRequest(GetObjectRequest.builder()
            .bucket(config.s3Bucket)
            .key(job.getStrId() + ".zip")
            .build())
        .build();
    return s3Presigner.presignGetObject(presignRequest).url().toString();
  }
}
