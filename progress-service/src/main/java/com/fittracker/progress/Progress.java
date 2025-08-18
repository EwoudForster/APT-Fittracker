package com.fittracker.progress;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "progress")
public class Progress {

  // Primary key (uniek ID voor progress-record)
  @Id
  private UUID id;

  // Koppeling naar de user (uniek → één progress-record per gebruiker)
  @Column(nullable = false, unique = true)
  private UUID userId;

  // Hoeveel workouts de user heeft afgerond
  @Column(nullable = false)
  private int workoutsCompleted;

  // Best lifts in JSON-vorm, opgeslagen als plain tekst
  // -> vermijdt JSONB issues tussen DB en JPA
  @Column(nullable = false, columnDefinition = "TEXT")
  private String bestLifts;

  // Laatste update tijdstip
  @Column(nullable = false)
  private Instant updatedAt;

  // Lege constructor verplicht voor JPA
  public Progress() { }

  // Convenience constructor
  public Progress(UUID id, UUID userId, int workoutsCompleted, String bestLifts, Instant updatedAt) {
    this.id = id;
    this.userId = userId;
    this.workoutsCompleted = workoutsCompleted;
    this.bestLifts = bestLifts;
    this.updatedAt = updatedAt;
  }

  // Voor het saven van een nieuw record → vul defaults in
  @PrePersist
  public void prePersist() {
    if (this.id == null) this.id = UUID.randomUUID(); // nieuw UUID
    if (this.bestLifts == null) this.bestLifts = "{}"; // lege JSON
    if (this.updatedAt == null) this.updatedAt = Instant.now(); // timestamp
  }

  // Voor update → zet altijd updatedAt opnieuw
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
