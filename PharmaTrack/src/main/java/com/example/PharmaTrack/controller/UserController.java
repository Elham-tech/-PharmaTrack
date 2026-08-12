package com.example.PharmaTrack.controller;

import com.example.PharmaTrack.entity.User;
import com.example.PharmaTrack.entity.Role;
import com.example.PharmaTrack.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

// Marks this class as a REST controller; all methods return JSON responses
@RestController
// Base URL path for all endpoints in this controller
@RequestMapping("/api/users")
// Allows requests from any origin (CORS) - restrict in production
@CrossOrigin(origins = "*")
// Role-based access: ADMIN only (user & role management)
@PreAuthorize("hasAuthority(@appAuthorities.admin)")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Handles HTTP POST requests to create a new resource
    @PostMapping
    // Deserializes the HTTP request body JSON into a User object
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        User createdUser = userService.createUser(user);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    // Handles HTTP PUT requests to update an existing resource by ID
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody User user) {
        User updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<User> patchUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) throws com.fasterxml.jackson.databind.JsonMappingException {
        User existing = userService.getUserById(id);
        new ObjectMapper().updateValue(existing, updates);
        return ResponseEntity.ok(userService.updateUser(id, existing));
    }

    // Handles HTTP GET requests to retrieve a single resource by ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    // Handles HTTP GET requests to retrieve a single resource by ID
    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(
            // Extracts the {id} value from the URL path
            @PathVariable String username) {
        User user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/role/{role}")
    public ResponseEntity<List<User>> getUsersByRole(
            // Extracts the {id} value from the URL path
            @PathVariable Role role) {
        List<User> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(users);
    }

    // Handles HTTP DELETE requests to remove a resource by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/check/username/{username}")
    public ResponseEntity<Boolean> checkUsername(
            // Extracts the {id} value from the URL path
            @PathVariable String username) {
        return ResponseEntity.ok(userService.existsByUsername(username));
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/check/email/{email}")
    public ResponseEntity<Boolean> checkEmail(
            // Extracts the {id} value from the URL path
            @PathVariable String email) {
        return ResponseEntity.ok(userService.existsByEmail(email));
    }
}
