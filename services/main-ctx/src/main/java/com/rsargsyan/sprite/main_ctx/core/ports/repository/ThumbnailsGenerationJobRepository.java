package com.rsargsyan.sprite.main_ctx.core.ports.repository;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ThumbnailsGenerationJobRepository extends JpaRepository<ThumbnailsGenerationJob, Long> {
  Optional<ThumbnailsGenerationJob> findByAccountIdAndId(Long accountId, Long id);
  Page<ThumbnailsGenerationJob> findByAccountId(Long accountId, Pageable pageable);

  @Modifying
  @Transactional
  @Query("UPDATE ThumbnailsGenerationJob j SET j.lastHeartbeatAt = :now WHERE j.id = :id")
  void updateHeartbeat(@Param("id") Long id, @Param("now") Instant now);

  @Modifying
  @Transactional
  @Query("UPDATE ThumbnailsGenerationJob j SET j.mqConfirmedAt = :now WHERE j.id = :id")
  void updateMqConfirmedAt(@Param("id") Long id, @Param("now") Instant now);

  @Modifying
  @Transactional
  @Query("UPDATE ThumbnailsGenerationJob j SET j.mqSentAt = :now, j.mqConfirmedAt = null WHERE j.id = :id")
  void updateMqSent(@Param("id") Long id, @Param("now") Instant now);

  @Query("SELECT j FROM ThumbnailsGenerationJob j WHERE j.status = 'QUEUED' AND j.mqConfirmedAt IS NULL AND j.mqSentAt < :threshold")
  List<ThumbnailsGenerationJob> findStuckQueuedJobs(@Param("threshold") Instant threshold);

@Query("SELECT j FROM ThumbnailsGenerationJob j WHERE j.status IN ('RECEIVED', 'IN_PROGRESS') AND j.lastHeartbeatAt < :threshold")
  List<ThumbnailsGenerationJob> findStuckJobs(@Param("threshold") Instant threshold);
}
