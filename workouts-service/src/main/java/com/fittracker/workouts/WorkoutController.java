package com.fittracker.workouts;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/workouts")
public class WorkoutController {
  private final WorkoutRepo repo;
  public WorkoutController(WorkoutRepo repo){ this.repo = repo; }

  @GetMapping
  public List<Workout> list(@RequestParam UUID userId,
                            @RequestParam(required=false) Instant from,
                            @RequestParam(required=false) Instant to){
    if(from != null && to != null){
      List<Workout> all = repo.findAll();
      List<Workout> res = new ArrayList<>();
      for (Workout w : all) {
        if (w.getUserId()!=null && w.getUserId().equals(userId)) {
          var d = w.getDate();
          if (d != null && !d.isBefore(from) && !d.isAfter(to)) res.add(w);
        }
      }
      return res;
    }
    List<Workout> all = repo.findAll();
    List<Workout> res = new ArrayList<>();
    for (Workout w : all) if (userId.equals(w.getUserId())) res.add(w);
    return res;
  }

  @GetMapping("/{id}") public Workout get(@PathVariable String id){ return repo.findById(id).orElseThrow(); }
  @PostMapping public Workout create(@RequestBody Workout w){ w.setId(null); return repo.save(w); }
  @PutMapping("/{id}") public Workout update(@PathVariable String id, @RequestBody Workout w){
    var cur = repo.findById(id).orElseThrow();
    if(w.getDate()!=null) cur.setDate(w.getDate());
    if(w.getExercises()!=null) cur.setExercises(w.getExercises());
    if(w.getUserId()!=null) cur.setUserId(w.getUserId());
    return repo.save(cur);
  }
  @DeleteMapping("/{id}") public void delete(@PathVariable String id){ repo.deleteById(id); }
}
