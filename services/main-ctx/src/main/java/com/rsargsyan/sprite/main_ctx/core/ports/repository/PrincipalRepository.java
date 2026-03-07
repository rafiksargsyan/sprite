package com.rsargsyan.sprite.main_ctx.core.ports.repository;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.Principal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrincipalRepository extends JpaRepository<Principal, Long> {
  List<Principal> findByExternalId(String externalId);
}
