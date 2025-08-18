package com.fittracker.progress;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProgressService {

  private final ProgressRepository repo;

  public ProgressService(ProgressRepository repo) {
    this.repo = repo;
  }

  public List<Progress> getAll() {
    return repo.findAll();
  }

  /** GET by user; maakt record aan als het nog niet bestaat. */
  public Progress getByUser(UUID userId) {
    return repo.findByUserId(userId).orElseGet(() -> {
      Progress p = new Progress();
      p.setUserId(userId);
      p.setWorkoutsCompleted(0);
      p.setBestLifts("{}");
      p.setUpdatedAt(Instant.now());
      return repo.save(p);
    });
  }

  /** Increment en updatedAt bijwerken — precies één save. */
  public Progress incrementWorkouts(UUID userId) {
    Progress p = repo.findByUserId(userId).orElse(null);

    if (p == null) {
      // bestond niet: maak meteen met waarde 1 en sla één keer op
      Progress created = new Progress();
      created.setUserId(userId);
      created.setWorkoutsCompleted(1);
      created.setBestLifts("{}");
      created.setUpdatedAt(Instant.now());
      return repo.save(created);
    } else {
      // bestond wel: verhoog en sla één keer op
      p.setWorkoutsCompleted(p.getWorkoutsCompleted() + 1);
      p.setUpdatedAt(Instant.now());
      return repo.save(p);
    }
  }

  /** Upsert vanuit body. */
  public Progress upsert(Progress body) {
    if (body.getUserId() == null) {
      throw new IllegalArgumentException("userId is required");
    }
    Progress existing = repo.findByUserId(body.getUserId()).orElse(null);
    if (existing == null) {
      body.setId(null); // laat prePersist een UUID zetten
      if (body.getBestLifts() == null) body.setBestLifts("{}");
      if (body.getUpdatedAt() == null) body.setUpdatedAt(Instant.now());
      // workoutsCompleted is primitive int -> default 0 als niet gezet
      return repo.save(body);
    } else {
      if (body.getBestLifts() != null) existing.setBestLifts(body.getBestLifts());
      existing.setWorkoutsCompleted(body.getWorkoutsCompleted());
      existing.setUpdatedAt(Instant.now());
      return repo.save(existing);
    }
  }
}
