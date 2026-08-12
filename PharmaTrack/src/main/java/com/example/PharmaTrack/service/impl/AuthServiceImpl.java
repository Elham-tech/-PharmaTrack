/*
 * ARCHITECTURE: Registration service. It accepts a username, password, and one or more
 * authority names (requirement 7). The password is ALWAYS encoded with the injected
 * PasswordEncoder before the user is saved (requirement 6) - plain-text passwords never
 * reach the database. Authorities are looked up in the authorities table and created on
 * the fly if they don't exist yet, so no authority name is hard-coded anywhere.
 */
package com.example.PharmaTrack.service.impl;

import com.example.PharmaTrack.dao.AuthorityDAO;
import com.example.PharmaTrack.dao.UserDAO;
import com.example.PharmaTrack.dto.RegisterRequest;
import com.example.PharmaTrack.entity.AuditLog.AuditAction;
import com.example.PharmaTrack.entity.Authority;
import com.example.PharmaTrack.entity.Role;
import com.example.PharmaTrack.entity.User;
import com.example.PharmaTrack.exception.BadRequestException;
import com.example.PharmaTrack.exception.ResourceNotFoundException;
import com.example.PharmaTrack.security.CurrentUserProvider;
import com.example.PharmaTrack.service.AuditLogService;
import com.example.PharmaTrack.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserDAO userDAO;
    private final AuthorityDAO authorityDAO;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final CurrentUserProvider currentUserProvider;

    // Constructor injection throughout (requirement 16)
    public AuthServiceImpl(UserDAO userDAO, AuthorityDAO authorityDAO, PasswordEncoder passwordEncoder,
                           AuditLogService auditLogService, CurrentUserProvider currentUserProvider) {
        this.userDAO = userDAO;
        this.authorityDAO = authorityDAO;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public User register(RegisterRequest request) {
        // Duplicate checks before any write
        if (userDAO.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists: " + request.getUsername());
        }
        if (userDAO.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists: " + request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        // Encode the password BEFORE saving (requirement 6)
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Resolve each requested authority name to an Authority entity (find or create).
        // Also derive the business Role enum if the authority matches a known pharmacy role,
        // so pharmacy UI features keep working for matching registrations.
        Role businessRole = null;
        Set<Authority> authorities = new HashSet<>();
        for (String rawName : request.getAuthorityNames()) {
            String name = rawName.trim().toUpperCase();
            if (name.isEmpty()) continue;
            Authority authority = authorityDAO.findByName(name);
            if (authority == null) {
                authority = new Authority(name);
                authorityDAO.save(authority);
            }
            authorities.add(authority);
            if (businessRole == null) {
                try {
                    businessRole = Role.valueOf(name);
                } catch (IllegalArgumentException ignored) {
                    // Generic authority name - no matching pharmacy role, role stays null
                }
            }
        }
        if (authorities.isEmpty()) {
            throw new BadRequestException("At least one valid authority name is required");
        }
        user.setAuthorities(authorities);
        user.setRole(businessRole);
        User saved = userDAO.save(user);
        // Registration is admin-only, so the audit entry records the admin who created the account.
        auditLogService.logAction("User", saved.getId(), AuditAction.CREATE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            null, "username=" + saved.getUsername());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser(String username) {
        User user = userDAO.findByUsernameWithAuthorities(username);
        if (user == null) {
            throw new ResourceNotFoundException("User", "username", username);
        }
        return user;
    }
}
