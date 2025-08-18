package com.fittracker.workouts;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface WorkoutRepo extends MongoRepository<Workout, String> {}
