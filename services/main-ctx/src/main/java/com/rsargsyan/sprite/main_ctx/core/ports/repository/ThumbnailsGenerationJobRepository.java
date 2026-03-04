package com.rsargsyan.sprite.main_ctx.core.ports.repository;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ThumbnailsGenerationJobRepository extends JpaRepository<ThumbnailsGenerationJob, Long> {
  Optional<ThumbnailsGenerationJob> findByAccountIdAndId(long accountId, long id);
}
