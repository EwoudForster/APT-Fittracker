package com.fittracker.progress;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/progress")
public class ProgressController {

  // Service laag die alle businesslogica bevat
  private final ProgressService service;

  // Constructor injection (aanbevolen manier in Spring)
  public ProgressController(ProgressService service) {
    this.service = service;
  }

  // Als userId meegegeven wordt → enkel die progress teruggeven
  // Anders → lijst van alle progress-records
  @GetMapping
  public ResponseEntity<?> get(
      @RequestParam(required = false) UUID userId
  ) {
    if (userId == null) {
      List<Progress> all = service.getAll();
      return ResponseEntity.ok(all);
    }
    return ResponseEntity.ok(service.getByUser(userId));
  }

  // Verhoogt workoutsCompleted voor deze user met +1
  @PutMapping("/{userId}/increment")
  public ResponseEntity<Progress> increment(@PathVariable UUID userId) {
    return ResponseEntity.ok(service.incrementWorkouts(userId));
  }

  // Upsert → maakt nieuw record als het niet bestaat,
  // update bestaand record als het er al is
  @PutMapping
  public ResponseEntity<Progress> upsert(@RequestBody Progress body) {
    return ResponseEntity.ok(service.upsert(body));
  }
}
