package com.fittracker.workouts;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WorkoutRepo extends MongoRepository<Workout, String> {

  // Zonder datumfilter
  List<Workout> findByUserId(UUID userId);

  // Alleen datumfilter (alle users)
  List<Workout> findByDateBetween(Instant from, Instant to);
  List<Workout> findByDateGreaterThanEqual(Instant from);
  List<Workout> findByDateLessThanEqual(Instant to);

  // User + datumfilter
  List<Workout> findByUserIdAndDateBetween(UUID userId, Instant from, Instant to);
  List<Workout> findByUserIdAndDateGreaterThanEqual(UUID userId, Instant from);
  List<Workout> findByUserIdAndDateLessThanEqual(UUID userId, Instant to);
}
