package com.fittracker.workouts;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Document("workouts") // Dit document komt terecht in de MongoDB collectie "workouts"
public class Workout {

  @Id // MongoDB document ID
  private String id;

  // User waarvoor de workout is
  private UUID userId;

  // Datum van de workout
  private Instant date;

  // Lijst met oefeningen die bij deze workout horen
  private List<Exercise> exercises;

  // Nested class om 1 oefening te beschrijven
  public static class Exercise {
    private String name;   // naam van de oefening
    private int sets;      // aantal sets
    private int reps;      // aantal herhalingen per set
    private Double weight; // gewicht in kg

    public Exercise() {}

    // Getters & setters zodat Spring + MongoDB de data kan vullen
    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }
    public int getSets(){ return sets; }
    public void setSets(int sets){ this.sets = sets; }
    public int getReps(){ return reps; }
    public void setReps(int reps){ this.reps = reps; }
    public Double getWeight(){ return weight; }
    public void setWeight(Double weight){ this.weight = weight; }
  }

  // Lege constructor (vereist voor MongoDB)
  public Workout() {}

  // Getters & setters voor de Workout zelf
  public String getId(){ return id; }
  public void setId(String id){ this.id = id; }

  public UUID getUserId(){ return userId; }
  public void setUserId(UUID userId){ this.userId = userId; }

  public Instant getDate(){ return date; }
  public void setDate(Instant date){ this.date = date; }

  public List<Exercise> getExercises(){ return exercises; }
  public void setExercises(List<Exercise> exercises){ this.exercises = exercises; }
}
