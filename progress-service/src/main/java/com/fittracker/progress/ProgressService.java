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

  // Constructor injection
  public ProgressService(ProgressRepository repo) {
    this.repo = repo;
  }

  // Haal ALLE progress-records op (zeldzaam, maar handig voor overzicht/debug)
  public List<Progress> getAll() {
    return repo.findAll();
  }

  // Haal progress op voor 1 user.
  // Als er nog geen record bestaat → maak er meteen eentje aan met default waarden
  public Progress getByUser(UUID userId) {
    return repo.findByUserId(userId).orElseGet(() -> {
      Progress p = new Progress();
      p.setUserId(userId);
      p.setWorkoutsCompleted(0);
      p.setBestLifts("{}");            // lege JSON als default
      p.setUpdatedAt(Instant.now());   // nu als timestamp
      return repo.save(p);
    });
  }

  // Verhoog workoutsCompleted met +1 (of maak record aan met waarde 1)
  // Belangrijk: slechts 1 save uitvoeren om overbodige DB calls te vermijden
  public Progress incrementWorkouts(UUID userId) {
    Progress p = repo.findByUserId(userId).orElse(null);

    if (p == null) {
      // Bestaat nog niet → nieuwe progress starten op 1
      Progress created = new Progress();
      created.setUserId(userId);
      created.setWorkoutsCompleted(1);
      created.setBestLifts("{}");
      created.setUpdatedAt(Instant.now());
      return repo.save(created);
    } else {
      // Bestaat wel → gewoon verhogen en opslaan
      p.setWorkoutsCompleted(p.getWorkoutsCompleted() + 1);
      p.setUpdatedAt(Instant.now());
      return repo.save(p);
    }
  }

  // Upsert logica:
  // - Als er nog geen record is → nieuw record maken
  // - Als er wel al één is → bijwerken
  public Progress upsert(Progress body) {
    if (body.getUserId() == null) {
      throw new IllegalArgumentException("userId is required");
    }

    Progress existing = repo.findByUserId(body.getUserId()).orElse(null);
    if (existing == null) {
      // Nog geen record → nieuwe maken
      body.setId(null); // laat @PrePersist de UUID zetten
      if (body.getBestLifts() == null) body.setBestLifts("{}");
      if (body.getUpdatedAt() == null) body.setUpdatedAt(Instant.now());
      // workoutsCompleted is int → default = 0 als niet gezet
      return repo.save(body);
    } else {
      // Record bestaat al → bijwerken
      if (body.getBestLifts() != null) existing.setBestLifts(body.getBestLifts());
      existing.setWorkoutsCompleted(body.getWorkoutsCompleted());
      existing.setUpdatedAt(Instant.now());
      return repo.save(existing);
    }
  }
}
