package com.fittracker.workouts;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/workouts")
public class WorkoutController {

  private final WorkoutRepo repo;

  public WorkoutController(WorkoutRepo repo) {
    this.repo = repo;
  }

  @GetMapping
  public List<Workout> list(
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to
  ) {
    // Geen filters -> alles
    if (userId == null && from == null && to == null) {
      return repo.findAll();
    }

    // Alleen userId
    if (userId != null && from == null && to == null) {
      return repo.findByUserId(userId);
    }

    // Alleen datum (alle users)
    if (userId == null) {
      if (from != null && to != null) return repo.findByDateBetween(from, to);
      if (from != null)               return repo.findByDateGreaterThanEqual(from);
      /* to != null */                return repo.findByDateLessThanEqual(to);
    }

    // userId + datum
    if (from != null && to != null)  return repo.findByUserIdAndDateBetween(userId, from, to);
    if (from != null)                return repo.findByUserIdAndDateGreaterThanEqual(userId, from);
    /* to != null */                 return repo.findByUserIdAndDateLessThanEqual(userId, to);
  }

  @GetMapping("/{id}")
  public Workout get(@PathVariable String id) {
    return repo.findById(id).orElseThrow();
  }

  @PostMapping
  public Workout create(@RequestBody Workout w) {
    w.setId(null);
    return repo.save(w);
  }

  @PutMapping("/{id}")
  public Workout update(@PathVariable String id, @RequestBody Workout w) {
    var cur = repo.findById(id).orElseThrow();
    if (w.getDate() != null)      cur.setDate(w.getDate());
    if (w.getExercises() != null) cur.setExercises(w.getExercises());
    if (w.getUserId() != null)    cur.setUserId(w.getUserId());
    return repo.save(cur);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    repo.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
