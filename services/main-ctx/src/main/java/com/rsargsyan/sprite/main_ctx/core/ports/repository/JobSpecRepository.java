package com.rsargsyan.sprite.main_ctx.core.ports.repository;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.JobSpec;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobSpecRepository extends JpaRepository<JobSpec, Long> {
  Optional<JobSpec> findByAccountIdAndId(Long accountId, Long id);
}
