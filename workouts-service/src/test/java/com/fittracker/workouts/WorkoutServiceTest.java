package com.fittracker.workouts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

  @Mock
  WorkoutRepo repo;

  @InjectMocks
  WorkoutService service;

  UUID u1 = UUID.randomUUID();
  UUID u2 = UUID.randomUUID();

  Workout w1_u1_now;
  Workout w2_u1_yesterday;
  Workout w3_u2_now;

  Instant now;
  Instant yesterday;
  Instant twoDaysAgo;

  @BeforeEach
  void setUp() {
    now = Instant.now();
    yesterday = now.minusSeconds(24 * 3600);
    twoDaysAgo = now.minusSeconds(2 * 24 * 3600);

    w1_u1_now = new Workout();
    w1_u1_now.setId("A");
    w1_u1_now.setUserId(u1);
    w1_u1_now.setDate(now);

    w2_u1_yesterday = new Workout();
    w2_u1_yesterday.setId("B");
    w2_u1_yesterday.setUserId(u1);
    w2_u1_yesterday.setDate(yesterday);

    w3_u2_now = new Workout();
    w3_u2_now.setId("C");
    w3_u2_now.setUserId(u2);
    w3_u2_now.setDate(now);
  }

  @Test
  void list_returnsAll_whenNoFilters() {
    when(repo.findAll()).thenReturn(List.of(w1_u1_now, w2_u1_yesterday, w3_u2_now));

    var res = service.list(null, null, null);

    assertEquals(3, res.size());
    verify(repo).findAll();
  }

  @Test
  void list_filtersByUser_onlyThatUsersWorkouts() {
    when(repo.findAll()).thenReturn(List.of(w1_u1_now, w2_u1_yesterday, w3_u2_now));

    var res = service.list(u1, null, null);

    assertEquals(2, res.size());
    assertTrue(res.stream().allMatch(w -> u1.equals(w.getUserId())));
  }

  @Test
  void list_filtersByUserAndDateRange_inclusive() {
    when(repo.findAll()).thenReturn(List.of(w1_u1_now, w2_u1_yesterday, w3_u2_now));

    // range die alleen "now" pakt, niet "yesterday"
    Instant from = now.minusSeconds(10);
    Instant to   = now.plusSeconds(10);

    var res = service.list(u1, from, to);

    assertEquals(1, res.size());
    assertEquals("A", res.get(0).getId());
  }

  @Test
  void get_returnsEntity_whenExists() {
    when(repo.findById("A")).thenReturn(Optional.of(w1_u1_now));

    var res = service.get("A");

    assertSame(w1_u1_now, res);
    verify(repo).findById("A");
  }

  @Test
  void get_throws_whenNotFound() {
    when(repo.findById("X")).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class, () -> service.get("X"));
    verify(repo).findById("X");
  }

  @Test
  void create_nullsIdAndSaves() {
    Workout incoming = new Workout();
    incoming.setId("SHOULD_BE_NULLIFIED");
    incoming.setUserId(u1);
    incoming.setDate(now);

    when(repo.save(any(Workout.class))).thenAnswer(inv -> inv.getArgument(0));

    var saved = service.create(incoming);

    ArgumentCaptor<Workout> captor = ArgumentCaptor.forClass(Workout.class);
    verify(repo).save(captor.capture());
    var passedToRepo = captor.getValue();
    assertNull(passedToRepo.getId(), "Service must null the id before save()");
    assertSame(saved, passedToRepo);
  }

  @Test
  void update_patchesNonNullFields() {
    Workout current = new Workout();
    current.setId("A");
    current.setUserId(u1);
    current.setDate(twoDaysAgo);

    when(repo.findById("A")).thenReturn(Optional.of(current));
    when(repo.save(any(Workout.class))).thenAnswer(inv -> inv.getArgument(0));

    Workout patch = new Workout();
    patch.setUserId(u2);          // verandert
    patch.setDate(yesterday);     // verandert
    patch.setExercises(null);     // blijft ongewijzigd

    var updated = service.update("A", patch);

    assertEquals(u2, updated.getUserId());
    assertEquals(yesterday, updated.getDate());
    verify(repo).save(updated);
  }

  @Test
  void delete_invokesRepository() {
    service.delete("X");
    verify(repo).deleteById("X");
  }
}
