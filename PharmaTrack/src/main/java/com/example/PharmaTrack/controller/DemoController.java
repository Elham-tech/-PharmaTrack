/*
 * ARCHITECTURE: Demo controller demonstrating the authentication/authorization split.
 * - /api/demo/public      -> public (no login required)
 * - /api/demo/authenticated -> any authenticated user
 * - /api/demo/admin       -> authenticated user WITH the ADMIN authority (configurable)
 * - /api/demo/pharmacist  -> authenticated user WITH the PHARMACIST authority (configurable)
 *
 * Method-level authorization (requirement 13) uses @PreAuthorize with SpEL expressions.
 * Authority names are NOT hard-coded: they are read from the configurable AppAuthorities
 * bean (populated from application.properties) and matched against the authorities loaded
 * from the database. An authenticated user without the required authority receives HTTP
 * 403 (requirement 14) via the accessDeniedHandler in SecurityConfig.
 */
package com.example.PharmaTrack.controller;

import com.example.PharmaTrack.security.AppAuthorities;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final AppAuthorities appAuthorities;

    // Constructor injection
    public DemoController(AppAuthorities appAuthorities) {
        this.appAuthorities = appAuthorities;
    }

    // Public - accessible without any authentication
    @GetMapping("/public")
    public Map<String, String> publicEndpoint() {
        return Map.of("message", "This endpoint is public - no login required");
    }

    // Any authenticated user
    @GetMapping("/authenticated")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> authenticatedEndpoint(Authentication authentication) {
        return Map.of(
            "message", "You are authenticated",
            "username", authentication.getName()
        );
    }

    // Authenticated + ADMIN authority (name comes from the configurable bean)
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority(@appAuthorities.admin)")
    public Map<String, String> adminEndpoint(Authentication authentication) {
        return Map.of(
            "message", "You have the ADMIN authority",
            "username", authentication.getName(),
            "requiredAuthority", appAuthorities.getAdmin()
        );
    }

    // Authenticated + PHARMACIST authority (name comes from the configurable bean)
    @GetMapping("/pharmacist")
    @PreAuthorize("hasAuthority(@appAuthorities.pharmacist)")
    public Map<String, String> pharmacistEndpoint(Authentication authentication) {
        return Map.of(
            "message", "You have the PHARMACIST authority",
            "username", authentication.getName(),
            "requiredAuthority", appAuthorities.getPharmacist()
        );
    }
}
