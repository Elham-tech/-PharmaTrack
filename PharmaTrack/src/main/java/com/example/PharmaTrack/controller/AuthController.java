/*
 * ARCHITECTURE: Authentication endpoints.
 * - POST /api/auth/register is ADMIN-ONLY (@PreAuthorize on the method): only authenticated
 *   ADMIN users can create accounts, so nobody can self-assign privileged roles. The password
 *   is encoded by AuthServiceImpl before persistence.
 * - GET /api/auth/me is PROTECTED: it only works for authenticated sessions and returns
 *   the logged-in user (the SPA uses it to decide who is signed in).
 * Login itself is handled by Spring Security's form login at POST /login (see SecurityConfig).
 */
package com.example.PharmaTrack.controller;

import com.example.PharmaTrack.dto.RegisterRequest;
import com.example.PharmaTrack.entity.User;
import com.example.PharmaTrack.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    // Constructor injection
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Registration is admin-only so that users cannot self-assign privileged roles
    @PostMapping("/register")
    @PreAuthorize("hasAuthority(@appAuthorities.admin)")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest request) {
        User created = authService.register(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // Protected endpoint: returns the currently authenticated user
    @GetMapping("/me")
    public ResponseEntity<User> me(Authentication authentication) {
        UserDetails details = (UserDetails) authentication.getPrincipal();
        User current = authService.getCurrentUser(details.getUsername());
        return ResponseEntity.ok(current);
    }
}
