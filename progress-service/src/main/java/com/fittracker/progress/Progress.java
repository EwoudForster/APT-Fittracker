package com.fittracker.progress;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "progress")
public class Progress {

  @Id
  private UUID id;

  @Column(nullable = false, unique = true)
  private UUID userId;

  @Column(nullable = false)
  private int workoutsCompleted;

  // Simpel & robuust: bewaar JSON als tekst (geen jsonb-cast-issues)
  @Column(nullable = false, columnDefinition = "TEXT")
  private String bestLifts;

  @Column(nullable = false)
  private Instant updatedAt;

  public Progress() {
    // JPA
  }

  public Progress(UUID id, UUID userId, int workoutsCompleted, String bestLifts, Instant updatedAt) {
    this.id = id;
    this.userId = userId;
    this.workoutsCompleted = workoutsCompleted;
    this.bestLifts = bestLifts;
    this.updatedAt = updatedAt;
  }

  @PrePersist
  public void prePersist() {
    if (this.id == null) this.id = UUID.randomUUID();
    if (this.bestLifts == null) this.bestLifts = "{}";
    if (this.updatedAt == null) this.updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = Instant.now();
  }

  // Getters / Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }

  public int getWorkoutsCompleted() { return workoutsCompleted; }
  public void setWorkoutsCompleted(int workoutsCompleted) { this.workoutsCompleted = workoutsCompleted; }

  public String getBestLifts() { return bestLifts; }
  public void setBestLifts(String bestLifts) { this.bestLifts = bestLifts; }

  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
