package com.rsargsyan.sprite.main_ctx.core.ports.repository;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
  List<ApiKey> findByUserProfileId(Long userProfileId);
  Optional<ApiKey> findByIdAndUserProfileId(Long id, Long userProfileId);
}
