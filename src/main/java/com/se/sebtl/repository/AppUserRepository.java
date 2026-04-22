package com.se.sebtl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.se.sebtl.model.AppUser;

// Fixed: The ID type must be Integer, not String.
public interface AppUserRepository extends JpaRepository<AppUser, Integer> {
    Optional<AppUser> findByUsername(String username);
}