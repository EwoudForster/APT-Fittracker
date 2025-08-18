package com.fittracker.workouts;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/workouts") // Alle endpoints van deze controller starten met /workouts
public class WorkoutController {

  private final WorkoutRepo repo;

  // Repo wordt via dependency injection binnengehaald
  public WorkoutController(WorkoutRepo repo) {
    this.repo = repo;
  }

  // lijst van workouts ophalen (optioneel gefilterd op userId of datum)
  @GetMapping
  public List<Workout> list(
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to
  ) {
    // Als er geen filters zijn -> alles teruggeven
    if (userId == null && from == null && to == null) {
      return repo.findAll();
    }

    // Alleen filteren op userId
    if (userId != null && from == null && to == null) {
      return repo.findByUserId(userId);
    }

    // Alleen filteren op datums (voor alle users)
    if (userId == null) {
      if (from != null && to != null) return repo.findByDateBetween(from, to);
      if (from != null)               return repo.findByDateGreaterThanEqual(from);
      /* to != null */                return repo.findByDateLessThanEqual(to);
    }

    // Zowel userId + datums
    if (from != null && to != null)  return repo.findByUserIdAndDateBetween(userId, from, to);
    if (from != null)                return repo.findByUserIdAndDateGreaterThanEqual(userId, from);
    /* to != null */                 return repo.findByUserIdAndDateLessThanEqual(userId, to);
  }

  // specifieke workout ophalen
  @GetMapping("/{id}")
  public Workout get(@PathVariable String id) {
    return repo.findById(id).orElseThrow(); // geeft 404 als niet gevonden
  }

  // nieuwe workout aanmaken
  @PostMapping
  public Workout create(@RequestBody Workout w) {
    w.setId(null); // Id leegmaken zodat Mongo er zelf één genereert
    return repo.save(w);
  }

  // bestaande workout updaten
  @PutMapping("/{id}")
  public Workout update(@PathVariable String id, @RequestBody Workout w) {
    var cur = repo.findById(id).orElseThrow(); // huidige versie ophalen

    // Enkel overschrijven wat in de request aanwezig is
    if (w.getDate() != null)      cur.setDate(w.getDate());
    if (w.getExercises() != null) cur.setExercises(w.getExercises());
    if (w.getUserId() != null)    cur.setUserId(w.getUserId());

    return repo.save(cur); // bewaren in de database
  }

  // workout verwijderen
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    repo.deleteById(id);
    return ResponseEntity.noContent().build(); // geeft 204 No Content terug
  }
}
