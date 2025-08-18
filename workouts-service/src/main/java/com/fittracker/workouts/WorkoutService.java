package com.fittracker.workouts;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class WorkoutService {

  private final WorkoutRepo repo;

  public WorkoutService(WorkoutRepo repo) {
    this.repo = repo;
  }


  public List<Workout> list(UUID userId, Instant from, Instant to) {
    List<Workout> all = repo.findAll();

    //  alles terug
    if (userId == null && from == null && to == null) {
      return all;
    }

    List<Workout> out = new ArrayList<>();
    for (Workout w : all) {
      // user filter
      if (userId != null) {
        if (w.getUserId() == null || !userId.equals(w.getUserId())) continue;
      }
      // daterange filter
      if (from != null && to != null) {
        Instant d = w.getDate();
        if (d == null) continue;
        if (d.isBefore(from) || d.isAfter(to)) continue;
      }
      out.add(w);
    }
    return out;
  }

  public Workout get(String id) {
    return repo.findById(id).orElseThrow(NoSuchElementException::new);
  }

  public Workout create(Workout w) {
    w.setId(null);               // laat MongoDB een id genereren
    return repo.save(w);
  }

  public Workout update(String id, Workout patch) {
    Workout cur = repo.findById(id).orElseThrow(NoSuchElementException::new);
    if (patch.getDate() != null)      cur.setDate(patch.getDate());
    if (patch.getExercises() != null) cur.setExercises(patch.getExercises());
    if (patch.getUserId() != null)    cur.setUserId(patch.getUserId());
    return repo.save(cur);
  }

  public void delete(String id) {
    repo.deleteById(id);
  }
}
