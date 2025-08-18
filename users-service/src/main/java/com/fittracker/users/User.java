package com.fittracker.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")  // Map deze klasse naar de tabel 'users'
public class User {
  @Id
  private UUID id;   // Primary key (UUID i.p.v. auto-increment)

  @Column(nullable=false, unique=true)
  private String email;   // Email moet verplicht én uniek zijn

  @Column(nullable=false)
  private String displayName;   // Naam die de gebruiker ziet

  private Instant createdAt;   // Wanneer de user is aangemaakt

  @PrePersist
  public void prePersist() {
    // Zorgt dat er automatisch een UUID en createdAt gezet wordt
    if (id == null) id = UUID.randomUUID();
    if (createdAt == null) createdAt = Instant.now();
  }

  // Lege constructor verplicht voor JPA
  public User() {}

  // Getters & Setters (standaard Java bean style)
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getDisplayName() { return displayName; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
