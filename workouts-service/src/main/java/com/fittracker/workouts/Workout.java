package com.fittracker.workouts;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Document("workouts")
public class Workout {
  @Id
  private String id;
  private UUID userId;
  private Instant date;
  private List<Exercise> exercises;

  public static class Exercise {
    private String name;
    private int sets;
    private int reps;
    private Double weight;

    public Exercise() {}
    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }
    public int getSets(){ return sets; }
    public void setSets(int sets){ this.sets = sets; }
    public int getReps(){ return reps; }
    public void setReps(int reps){ this.reps = reps; }
    public Double getWeight(){ return weight; }
    public void setWeight(Double weight){ this.weight = weight; }
  }

  public Workout() {}

  public String getId(){ return id; }
  public void setId(String id){ this.id = id; }
  public UUID getUserId(){ return userId; }
  public void setUserId(UUID userId){ this.userId = userId; }
  public Instant getDate(){ return date; }
  public void setDate(Instant date){ this.date = date; }
  public List<Exercise> getExercises(){ return exercises; }
  public void setExercises(List<Exercise> exercises){ this.exercises = exercises; }
}
