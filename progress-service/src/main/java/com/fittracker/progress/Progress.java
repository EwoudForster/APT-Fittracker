package com.fittracker.progress;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "progress")
public class Progress {
  @Id
  private UUID id;
  @Column(nullable=false)
  private UUID userId;
  private int workoutsCompleted;
  @Column(columnDefinition="jsonb")
  private String bestLifts;
  private Instant updatedAt;

  @PrePersist
  public void prePersist(){
    if (id == null) id = UUID.randomUUID();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  public Progress() {}

  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id = id; }
  public UUID getUserId(){ return userId; }
  public void setUserId(UUID userId){ this.userId = userId; }
  public int getWorkoutsCompleted(){ return workoutsCompleted; }
  public void setWorkoutsCompleted(int workoutsCompleted){ this.workoutsCompleted = workoutsCompleted; }
  public String getBestLifts(){ return bestLifts; }
  public void setBestLifts(String bestLifts){ this.bestLifts = bestLifts; }
  public Instant getUpdatedAt(){ return updatedAt; }
  public void setUpdatedAt(Instant updatedAt){ this.updatedAt = updatedAt; }
}
