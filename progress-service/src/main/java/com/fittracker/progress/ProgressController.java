package com.fittracker.progress;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.UUID;

@RestController
@RequestMapping("/progress")
public class ProgressController {
  private final ProgressService svc;
  public ProgressController(ProgressService svc){ this.svc = svc; }

  @GetMapping
  public Progress get(@RequestParam UUID userId){ return svc.getByUser(userId); }

  @PutMapping("/{userId}/increment")
  public Progress inc(@PathVariable UUID userId){ return svc.incrementWorkouts(userId); }

  @PutMapping
  public Progress save(@RequestBody Progress p){ return svc.save(p); }
}
