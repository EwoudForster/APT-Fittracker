package com.fittracker.progress;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

  @Mock
  ProgressRepository progressRepository;

  @InjectMocks
  ProgressService service;

  UUID u1;
  Progress existing;

  @BeforeEach
  void init() {
    u1 = UUID.randomUUID();
    existing = new Progress(
        UUID.randomUUID(),
        u1,
        3,
        "{}",
        Instant.now()
    );
  }

  @Test
  void getAll_returnsAll() {
    var list = List.of(existing);
    when(progressRepository.findAll()).thenReturn(list);

    var res = service.getAll();

    assertSame(list, res);
    verify(progressRepository).findAll();
  }

  @Test
  void getByUser_returnsExisting_withoutSaving() {
    when(progressRepository.findByUserId(u1)).thenReturn(Optional.of(existing));

    var res = service.getByUser(u1);

    assertSame(existing, res);
    verify(progressRepository).findByUserId(u1);
    verify(progressRepository, never()).save(any());
  }

  @Test
  void getByUser_createsWhenMissing_withZeroAndDefaults() {
    when(progressRepository.findByUserId(u1)).thenReturn(Optional.empty());
    when(progressRepository.save(any(Progress.class))).thenAnswer(inv -> inv.getArgument(0));

    var res = service.getByUser(u1);

    assertEquals(u1, res.getUserId());
    assertEquals(0, res.getWorkoutsCompleted());
    assertNotNull(res.getUpdatedAt());
    assertEquals("{}", res.getBestLifts());

    ArgumentCaptor<Progress> cap = ArgumentCaptor.forClass(Progress.class);
    verify(progressRepository).save(cap.capture());
    assertEquals(0, cap.getValue().getWorkoutsCompleted());
  }

  @Test
  void incrementWorkouts_incrementsExisting_inSingleSave() {
    when(progressRepository.findByUserId(u1)).thenReturn(Optional.of(existing));
    when(progressRepository.save(any(Progress.class))).thenAnswer(inv -> inv.getArgument(0));

    var res = service.incrementWorkouts(u1);

    assertEquals(4, res.getWorkoutsCompleted());

    ArgumentCaptor<Progress> cap = ArgumentCaptor.forClass(Progress.class);
    verify(progressRepository).save(cap.capture());
    assertEquals(4, cap.getValue().getWorkoutsCompleted());
    assertNotNull(cap.getValue().getUpdatedAt());
  }

  @Test
  void incrementWorkouts_createsThenIncrementsWhenMissing_twoCallsTwoSaves() {
    // 1e call: niets in DB
    // 2e call: record bestaat met waarde 1
    Progress afterFirst = new Progress(UUID.randomUUID(), u1, 1, "{}", Instant.now());

    when(progressRepository.findByUserId(u1))
        .thenReturn(Optional.empty(), Optional.of(afterFirst));
    when(progressRepository.save(any(Progress.class))).thenAnswer(inv -> inv.getArgument(0));

    // eerste increment: maakt record met 1
    var p1 = service.incrementWorkouts(u1);
    assertEquals(1, p1.getWorkoutsCompleted());

    // tweede increment: maakt 2
    var p2 = service.incrementWorkouts(u1);
    assertEquals(2, p2.getWorkoutsCompleted());

    ArgumentCaptor<Progress> cap = ArgumentCaptor.forClass(Progress.class);
    verify(progressRepository, times(2)).save(cap.capture());
    var saved = cap.getAllValues();
    assertEquals(1, saved.get(0).getWorkoutsCompleted());
    assertEquals(2, saved.get(1).getWorkoutsCompleted());
  }

  @Test
  void upsert_createsWhenAbsent_defaultsFilled() {
    Progress body = new Progress();
    body.setUserId(u1);                 // geen id, geen bestLifts, geen updatedAt, workoutsCompleted blijft default 0

    when(progressRepository.findByUserId(u1)).thenReturn(Optional.empty());
    when(progressRepository.save(any(Progress.class))).thenAnswer(inv -> inv.getArgument(0));

    var res = service.upsert(body);

    assertEquals(u1, res.getUserId());
    assertEquals(0, res.getWorkoutsCompleted());
    assertEquals("{}", res.getBestLifts());
    assertNotNull(res.getUpdatedAt());

    ArgumentCaptor<Progress> cap = ArgumentCaptor.forClass(Progress.class);
    verify(progressRepository).save(cap.capture());
    assertNull(cap.getValue().getId(), "id moet door JPA/@PrePersist gezet worden");
  }

  @Test
  void upsert_updatesWhenPresent_overwritesFieldsAndTimestamp() {
    Progress present = new Progress(UUID.randomUUID(), u1, 5, "{}", Instant.now().minusSeconds(60));
    Progress body = new Progress();
    body.setUserId(u1);
    body.setWorkoutsCompleted(9);
    body.setBestLifts("{\"bench\":100}");

    when(progressRepository.findByUserId(u1)).thenReturn(Optional.of(present));
    when(progressRepository.save(any(Progress.class))).thenAnswer(inv -> inv.getArgument(0));

    var res = service.upsert(body);

    assertEquals(9, res.getWorkoutsCompleted());
    assertEquals("{\"bench\":100}", res.getBestLifts());
    assertNotNull(res.getUpdatedAt());

    ArgumentCaptor<Progress> cap = ArgumentCaptor.forClass(Progress.class);
    verify(progressRepository).save(cap.capture());
    assertEquals(9, cap.getValue().getWorkoutsCompleted());
    assertEquals("{\"bench\":100}", cap.getValue().getBestLifts());
  }
}
