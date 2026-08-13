package com.example.PharmaTrack.service;

import com.example.PharmaTrack.entity.User;
import com.example.PharmaTrack.entity.Role;

import java.util.List;

public interface UserService {
    User createUser(User user);
    User updateUser(Long id, User user);
    User getUserById(Long id);
    User getUserByUsername(String username);
    List<User> getAllUsers();
    List<User> getUsersByRole(Role role);
    void deleteUser(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
