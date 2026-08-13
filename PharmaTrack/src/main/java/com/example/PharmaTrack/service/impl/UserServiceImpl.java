/*
 * ARCHITECTURE: Service layer sits between controller and DAO. It owns all business rules
 * (duplicate checks, business validations) and transaction boundaries. @Transactional at
 * class level means every method runs inside a database transaction by default - if an
 * exception is thrown, all changes are rolled back. @Transactional(readOnly = true) on read
 * methods tells Hibernate to skip dirty checking and use a read-only connection, improving
 * performance for queries that don't modify data.
 */
package com.example.PharmaTrack.service.impl;

import com.example.PharmaTrack.entity.AuditLog.AuditAction;
import com.example.PharmaTrack.entity.User;
import com.example.PharmaTrack.entity.Role;
import com.example.PharmaTrack.entity.Authority;
import com.example.PharmaTrack.dao.UserDAO;
import com.example.PharmaTrack.dao.AuthorityDAO;
import com.example.PharmaTrack.security.CurrentUserProvider;
import com.example.PharmaTrack.service.AuditLogService;
import com.example.PharmaTrack.service.UserService;
import com.example.PharmaTrack.exception.ResourceNotFoundException;
import com.example.PharmaTrack.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Marks this class as a Spring-managed service (business logic layer)
@Service
// Makes all methods transactional by default (auto-commits or rolls back on exception)
@Transactional
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final AuthorityDAO authorityDAO;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final CurrentUserProvider currentUserProvider;

    @Autowired
    public UserServiceImpl(UserDAO userDAO, AuthorityDAO authorityDAO, PasswordEncoder passwordEncoder,
                           AuditLogService auditLogService, CurrentUserProvider currentUserProvider) {
        this.userDAO = userDAO;
        this.authorityDAO = authorityDAO;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.currentUserProvider = currentUserProvider;
    }

    // Indicates this method implements an interface contract
    @Override
    public User createUser(User user) {
        if (userDAO.existsByUsername(user.getUsername())) {
            throw new BadRequestException("Username already exists: " + user.getUsername());
        }
        if (userDAO.existsByEmail(user.getEmail())) {
            throw new BadRequestException("Email already exists: " + user.getEmail());
        }
        // SECURITY: always encode the password before saving (requirement 6)
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Keep the user's authorities in sync with their business role so they can log in
        syncAuthoritiesFromRole(user);
        User saved = userDAO.save(user);
        auditLogService.logAction("User", saved.getId(), AuditAction.CREATE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            null, "username=" + saved.getUsername());
        return saved;
    }

    // Indicates this method implements an interface contract
    @Override
    public User updateUser(Long id, User user) {
        User existingUser = userDAO.findById(id);
        if (existingUser == null) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        existingUser.setFullName(user.getFullName());
        existingUser.setRole(user.getRole());
        existingUser.setActive(user.isActive());
        // Re-encode the password only when a new one was actually provided
        if (user.getPassword() != null && !user.getPassword().isBlank()
                && !passwordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        syncAuthoritiesFromRole(existingUser);
        User saved = userDAO.save(existingUser);
        auditLogService.logAction("User", id, AuditAction.UPDATE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            null, "username=" + saved.getUsername());
        return saved;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        User user = userDAO.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        return user;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        User user = userDAO.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("User", "username", username);
        }
        return user;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(Role role) {
        return userDAO.findByRole(role);
    }

    // Indicates this method implements an interface contract
    @Override
    public void deleteUser(Long id) {
        if (!userDAO.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        userDAO.deleteById(id);
        auditLogService.logAction("User", id, AuditAction.DELETE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            "id=" + id, null);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userDAO.existsByUsername(username);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userDAO.existsByEmail(email);
    }

    /*
     * Ensures the user has an Authority matching their business Role (finding or creating
     * the authority row as needed). This keeps the DB-driven security model consistent
     * with the legacy role field used by the pharmacy UI.
     */
    private void syncAuthoritiesFromRole(User user) {
        if (user.getRole() == null) {
            return;
        }
        Set<Authority> authorities = user.getAuthorities();
        if (authorities == null) {
            authorities = new HashSet<>();
        }
        String authorityName = user.getRole().name();
        Authority authority = authorityDAO.findByName(authorityName);
        if (authority == null) {
            authority = new Authority(authorityName);
            authorityDAO.save(authority);
        }
        authorities.add(authority);
        user.setAuthorities(authorities);
    }
}
