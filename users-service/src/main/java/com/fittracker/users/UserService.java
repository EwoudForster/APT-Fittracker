package com.fittracker.users;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
  private final UserRepository repo;
  public UserService(UserRepository repo){ this.repo = repo; }

  public List<User> list(String email){
    if(email != null && !email.isBlank()){
      return repo.findByEmail(email).map(List::of).orElseGet(List::of);
    }
    return repo.findAll();
  }

  public User get(UUID id){ return repo.findById(id).orElseThrow(); }

  public User create(User u){ return repo.save(u); }

  public User update(UUID id, User patch){
    var cur = get(id);
    if(patch.getEmail()!=null) cur.setEmail(patch.getEmail());
    if(patch.getDisplayName()!=null) cur.setDisplayName(patch.getDisplayName());
    return repo.save(cur);
  }

  public void delete(UUID id){ repo.deleteById(id); }
}
