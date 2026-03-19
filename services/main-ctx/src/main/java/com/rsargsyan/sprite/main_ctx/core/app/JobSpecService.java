package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.core.Util;
import com.rsargsyan.sprite.main_ctx.core.app.dto.*;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.JobSpec;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.*;
import com.rsargsyan.sprite.main_ctx.core.exception.InvalidThumbnailConfigException;
import com.rsargsyan.sprite.main_ctx.core.exception.ResourceNotFoundException;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.AccountRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.JobSpecRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobSpecService {

  private final JobSpecRepository jobSpecRepository;
  private final AccountRepository accountRepository;

  @Autowired
  public JobSpecService(JobSpecRepository jobSpecRepository, AccountRepository accountRepository) {
    this.jobSpecRepository = jobSpecRepository;
    this.accountRepository = accountRepository;
  }

  @Transactional
  public JobSpecDTO create(String accountId, JobSpecCreationDTO dto) {
    var account = accountRepository.findById(Util.validateTSID(accountId))
        .orElseThrow(ResourceNotFoundException::new);
    if (jobSpecRepository.countByAccountId(account.getId()) >= 100)
      throw new InvalidThumbnailConfigException("Job spec limit of 100 reached");
    if (jobSpecRepository.existsByAccountIdAndName(account.getId(), dto.getName().trim()))
      throw new InvalidThumbnailConfigException("A job spec with this name already exists");
    var configs = dto.getConfigs().stream().map(JobSpecService::toConfig).toList();
    var jobSpec = new JobSpec(account, dto.getName(), dto.getDescription(), configs);
    jobSpecRepository.save(jobSpec);
    return JobSpecDTO.from(jobSpec);
  }

  @Transactional
  public void delete(String accountId, String jobSpecId) {
    var jobSpec = jobSpecRepository
        .findByAccountIdAndId(Util.validateTSID(accountId), Util.validateTSID(jobSpecId))
        .orElseThrow(ResourceNotFoundException::new);
    jobSpecRepository.delete(jobSpec);
  }

  public List<JobSpecDTO> findAll(String accountId) {
    return jobSpecRepository.findByAccountId(Util.validateTSID(accountId))
        .stream().map(JobSpecDTO::from).toList();
  }

  public JobSpecDTO findById(String accountId, String jobSpecId) {
    return jobSpecRepository
        .findByAccountIdAndId(Util.validateTSID(accountId), Util.validateTSID(jobSpecId))
        .map(JobSpecDTO::from)
        .orElseThrow(ResourceNotFoundException::new);
  }

  private static ThumbnailConfig toConfig(ThumbnailConfigRequest req) {
    if (req instanceof JpgThumbnailConfigRequest r) {
      return new JpgThumbnailConfig(r.resolution(), toSpriteSize(r.spriteSize()), r.quality(), r.interval(), r.folderName());
    } else if (req instanceof WebpThumbnailConfigRequest r) {
      return new WebpThumbnailConfig(r.resolution(), toSpriteSize(r.spriteSize()), r.quality(), r.method(), r.lossless(), r.interval(), r.preset(), r.folderName());
    } else if (req instanceof AvifThumbnailConfigRequest r) {
      return new AvifThumbnailConfig(r.resolution(), toSpriteSize(r.spriteSize()), r.quality(), r.interval(), r.speed(), r.folderName());
    } else if (req instanceof BlurhashThumbnailConfigRequest r) {
      return new BlurhashThumbnailConfig(32, r.interval(), r.componentsX(), r.componentsY(), r.folderName());
    }
    throw new IllegalStateException("Unknown config type: " + req.getClass());
  }

  private static SpriteSize toSpriteSize(SpriteSizeRequest req) {
    return new SpriteSize(req.rows(), req.cols());
  }
}
