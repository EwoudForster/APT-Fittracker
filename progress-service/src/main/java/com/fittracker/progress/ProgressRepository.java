package com.fittracker.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ProgressRepository extends JpaRepository<Progress, UUID> {
  Optional<Progress> findByUserId(UUID userId);
}
