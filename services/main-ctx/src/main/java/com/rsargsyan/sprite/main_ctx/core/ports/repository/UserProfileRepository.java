package com.rsargsyan.sprite.main_ctx.core.ports.repository;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
  List<UserProfile> findByPrincipalIdAndAccountId(Long principalId, Long accountId);
  List<UserProfile> findByPrincipalId(Long principalId);
}
