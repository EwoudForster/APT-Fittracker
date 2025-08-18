package com.fittracker.users;

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
class UserServiceTest {

  @Mock
  UserRepository repo;

  @InjectMocks
  UserService service;

  UUID id1;
  User u1;

  @BeforeEach
  void setUp() {
    id1 = UUID.randomUUID();
    u1 = new User();
    u1.setId(id1);
    u1.setEmail("alice@example.com");
    u1.setDisplayName("Alice");
    u1.setCreatedAt(Instant.now());
  }

  @Test
  void list_returnsAll_whenEmailNull() {
    var u2 = new User();
    u2.setId(UUID.randomUUID());
    u2.setEmail("bob@example.com");
    u2.setDisplayName("Bob");

    when(repo.findAll()).thenReturn(List.of(u1, u2));

    var res = service.list(null);

    assertEquals(2, res.size());
    verify(repo).findAll();
    verify(repo, never()).findByEmail(anyString());
  }

  @Test
  void list_returnsSingle_whenEmailProvidedAndFound() {
    when(repo.findByEmail("alice@example.com")).thenReturn(Optional.of(u1));

    var res = service.list("alice@example.com");

    assertEquals(1, res.size());
    assertEquals(u1, res.get(0));
    verify(repo).findByEmail("alice@example.com");
    verify(repo, never()).findAll();
  }

  @Test
  void list_returnsEmpty_whenEmailProvidedAndNotFound() {
    when(repo.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

    var res = service.list("nobody@example.com");

    assertTrue(res.isEmpty());
    verify(repo).findByEmail("nobody@example.com");
    verify(repo, never()).findAll();
  }

  @Test
  void get_returnsUser_whenExists() {
    when(repo.findById(id1)).thenReturn(Optional.of(u1));

    var res = service.get(id1);

    assertSame(u1, res);
    verify(repo).findById(id1);
  }

  @Test
  void get_throws_whenNotFound() {
    when(repo.findById(id1)).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class, () -> service.get(id1));
    verify(repo).findById(id1);
  }

  @Test
  void create_savesAndReturns() {
    var newUser = new User();
    newUser.setEmail("new@example.com");
    newUser.setDisplayName("Newbie");

    when(repo.save(newUser)).thenReturn(newUser);

    var res = service.create(newUser);

    assertSame(newUser, res);
    verify(repo).save(newUser);
  }

  @Test
  void update_patchesEmailAndDisplayName() {
    when(repo.findById(id1)).thenReturn(Optional.of(u1));
    when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    var patch = new User();
    patch.setEmail("alice+patched@example.com");
    patch.setDisplayName("Alice Patched");

    var updated = service.update(id1, patch);

    assertEquals("alice+patched@example.com", updated.getEmail());
    assertEquals("Alice Patched", updated.getDisplayName());

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(repo).save(captor.capture());
    var saved = captor.getValue();
    assertEquals(updated.getEmail(), saved.getEmail());
    assertEquals(updated.getDisplayName(), saved.getDisplayName());
  }

  @Test
  void update_onlyChangesProvidedFields() {
    when(repo.findById(id1)).thenReturn(Optional.of(u1));
    when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    var oldEmail = u1.getEmail();
    var patch = new User();
    patch.setDisplayName("Alice NEW"); // email blijft null → niet overschrijven

    var updated = service.update(id1, patch);

    assertEquals(oldEmail, updated.getEmail());
    assertEquals("Alice NEW", updated.getDisplayName());
  }

  @Test
  void delete_callsRepository() {
    doNothing().when(repo).deleteById(id1);

    service.delete(id1);

    verify(repo).deleteById(id1);
  }
}
