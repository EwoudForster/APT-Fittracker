package com.fittracker.progress;

import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class ProgressService {
  private final ProgressRepository repo;
  public ProgressService(ProgressRepository repo){ this.repo = repo; }

  public Progress getByUser(UUID userId){
    return repo.findByUserId(userId).orElseGet(() -> {
      Progress p = new Progress();
      p.setUserId(userId);
      p.setWorkoutsCompleted(0);
      p.setBestLifts("{}");
      return repo.save(p);
    });
  }

  public Progress incrementWorkouts(UUID userId){
    Progress p = getByUser(userId);
    p.setWorkoutsCompleted(p.getWorkoutsCompleted()+1);
    return repo.save(p);
  }

  public Progress save(Progress p){ return repo.save(p); }
}
