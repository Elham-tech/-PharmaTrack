package com.example.PharmaTrack.dao;

import com.example.PharmaTrack.entity.User;
import com.example.PharmaTrack.entity.Role;

import java.util.List;

public interface UserDAO {
    User findByUsername(String username);
    // Loads the user with its authorities eagerly (used by Spring Security's UserDetailsService)
    User findByUsernameWithAuthorities(String username);
    User findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByActive(boolean active);
    User findById(Long id);
    List<User> findAll();
    User save(User user);
    void deleteById(Long id);
    boolean existsById(Long id);
}
