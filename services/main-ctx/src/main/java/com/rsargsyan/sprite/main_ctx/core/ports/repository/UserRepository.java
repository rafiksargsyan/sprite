package com.rsargsyan.sprite.main_ctx.core.ports.repository;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserProfile, Long> {
}
