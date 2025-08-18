package com.fittracker.users;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")  // Alle endpoints beginnen met /users
public class UserController {
  private final UserService svc;

  // zo wordt de service binnengehaald
  public UserController(UserService svc){ 
    this.svc = svc; 
  }

  // users ophalen, kan met email filter
  @GetMapping
  public List<User> list(@RequestParam(required=false) String email){
    return svc.list(email);
  }

  // user ophalen op basis van UUID
  @GetMapping("/{id}")
  public User get(@PathVariable UUID id){ 
    return svc.get(id); 
  }

  // nieuwe user aanmaken
  @PostMapping
  public User create(@RequestBody @Valid User u){ 
    return svc.create(u); 
  }

  // bestaande user bijwerken
  @PutMapping("/{id}")
  public User update(@PathVariable UUID id, @RequestBody User u){ 
    return svc.update(id, u); 
  }

  // user verwijderen
  @DeleteMapping("/{id}")
  public void delete(@PathVariable UUID id){ 
    svc.delete(id); 
  }
}
