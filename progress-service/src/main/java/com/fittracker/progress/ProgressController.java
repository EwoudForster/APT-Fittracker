package com.fittracker.progress;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/progress")
public class ProgressController {

  private final ProgressService service;

  public ProgressController(ProgressService service) {
    this.service = service;
  }

  /**
   * GET /progress                   -> alle progress (lijst)
   * GET /progress?userId=<uuid>     -> progress voor 1 user (object, auto-create)
   */
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

  /** PUT /progress/{userId}/increment -> +1 workoutsCompleted */
  @PutMapping("/{userId}/increment")
  public ResponseEntity<Progress> increment(@PathVariable UUID userId) {
    return ResponseEntity.ok(service.incrementWorkouts(userId));
  }

  /** PUT /progress (upsert) */
  @PutMapping
  public ResponseEntity<Progress> upsert(@RequestBody Progress body) {
    return ResponseEntity.ok(service.upsert(body));
  }
}
