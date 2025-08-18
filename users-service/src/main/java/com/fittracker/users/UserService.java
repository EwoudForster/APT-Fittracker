package com.fittracker.users;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service  // Dit maakt er een Spring service van (business logica)
public class UserService {
  private final UserRepository repo;

  // Constructor injection van de repository
  public UserService(UserRepository repo){ 
    this.repo = repo; 
  }

  // Alle users ophalen (of 1 user zoeken op email als filter is meegegeven)
  public List<User> list(String email){
    if(email != null && !email.isBlank()){
      // probeer email te vinden, stop in lijstje of geef lege lijst terug
      return repo.findByEmail(email).map(List::of).orElseGet(List::of);
    }
    // geef gewoon alles
    return repo.findAll();
  }

  // Eén user ophalen op basis van UUID
  public User get(UUID id){ 
    return repo.findById(id).orElseThrow(); 
  }

  // Nieuwe user aanmaken
  public User create(User u){ 
    return repo.save(u); 
  }

  // User bijwerken
  public User update(UUID id, User patch){
    var cur = get(id); // Eerst de huidige user ophalen
    if(patch.getEmail()!=null)        cur.setEmail(patch.getEmail());
    if(patch.getDisplayName()!=null)  cur.setDisplayName(patch.getDisplayName());
    return repo.save(cur);
  }

  // User verwijderen
  public void delete(UUID id){ 
    repo.deleteById(id); 
  }
}
