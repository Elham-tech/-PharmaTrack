/*
 * ARCHITECTURE: Central Spring Security configuration for Spring Security 6.
 *
 * - SecurityFilterChain (NOT the deprecated WebSecurityConfigurerAdapter) builds the
 *   filter chain declaratively using the HttpSecurity DSL.
 * - PasswordEncoder bean is injected wherever passwords are persisted so they are
 *   always BCrypt-hashed before saving.
 * - @EnableMethodSecurity activates @PreAuthorize on controller/service methods.
 * - NO role or authority names are hard-coded in this file: URL rules only decide
 *   "public vs authenticated". Which authorities a user needs for a given operation
 *   is decided by @PreAuthorize expressions that reference the configurable
 *   AppAuthorities bean, whose values are matched against authorities read from the DB.
 * - Form login + logout are enabled per requirement. CSRF is disabled because this is
 *   an API-first SPA using session cookies; document this trade-off for production.
 */
package com.example.PharmaTrack.security;

import com.example.PharmaTrack.dao.UserDAO;
import com.example.PharmaTrack.dto.ErrorResponse;
import com.example.PharmaTrack.entity.AuditLog.AuditAction;
import com.example.PharmaTrack.entity.User;
import com.example.PharmaTrack.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// Marks this class as a Spring configuration class
@Configuration
// Enables Spring Security's web security support (registers the security filter chain)
@EnableWebSecurity
// Enables method-level security: activates @PreAuthorize / @Secured annotations
@EnableMethodSecurity
public class SecurityConfig {

    /*
     * PasswordEncoder bean (Spring Security 6 requirement). BCrypt adds a random salt
     * per hash, making rainbow-table attacks impractical. Every password is encoded
     * with this bean BEFORE being saved to the database.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*
     * The security filter chain. This replaces the old WebSecurityConfigurerAdapter
     * pattern. Constructor/parameter injection of ObjectMapper lets the JSON error
     * handlers serialize responses consistently with the rest of the API.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper,
                                                  UserDAO userDAO, AuditLogService auditLogService) throws Exception {

        http
            // CSRF disabled: this application is an API-first SPA that authenticates via
            // session cookies and sends no CSRF tokens. (Re-enable + token strategy for a
            // purely server-rendered deployment.)
            .csrf(csrf -> csrf.disable())

            // ---- URL-level authorization: only "public vs requires login" is decided here ----
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - reachable without any authentication
                .requestMatchers("/login", "/error", "/favicon.ico").permitAll()
                // Public demo endpoint (demonstrates the public side of the security split)
                .requestMatchers("/api/demo/public").permitAll()
                // Everything else (all pharmacy API endpoints) requires authentication
                .anyRequest().authenticated()
            )

            // ---- Form login (requirement) ----
            // POST /api/auth/register is NOT public: only ADMIN users may create accounts
            // (enforced by @PreAuthorize on AuthController.register).
            // GET /login renders Spring's default login form; POST /login authenticates.
            .formLogin(form -> form
                // Success: return JSON 200 instead of redirecting, so the SPA can act on it
                .successHandler((request, response, authentication) -> {
                    logAuditEvent(auditLogService, userDAO, authentication, AuditAction.LOGIN, request.getRemoteAddr());
                    response.setStatus(200);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Login successful\"}");
                })
                // Failure: return JSON 401 instead of redirecting to /login?error
                .failureHandler((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    objectMapper.writeValue(response.getWriter(),
                        new ErrorResponse(401, "Unauthorized", "Invalid username or password", request.getRequestURI()));
                })
            )

            // ---- Logout (requirement) ----
            // POST /logout invalidates the session. Returns 204 so the SPA can react.
            .logout(logout -> logout
                .logoutSuccessHandler((request, response, authentication) -> {
                    if (authentication != null) {
                        logAuditEvent(auditLogService, userDAO, authentication, AuditAction.LOGOUT, request.getRemoteAddr());
                    }
                    response.setStatus(204);
                })
            )

            // ---- Exception handling: JSON responses for the SPA ----
            .exceptionHandling(ex -> ex
                // Unauthenticated request to a protected endpoint -> 401 JSON
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    objectMapper.writeValue(response.getWriter(),
                        new ErrorResponse(401, "Unauthorized",
                            "Authentication is required to access this resource", request.getRequestURI()));
                })
                // Authenticated user WITHOUT the required authority -> 403 JSON (requirement 14)
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    objectMapper.writeValue(response.getWriter(),
                        new ErrorResponse(403, "Forbidden",
                            "You do not have the required authority to access this resource", request.getRequestURI()));
                })
            );

        return http.build();
    }

    /*
     * Records a LOGIN/LOGOUT audit trail entry for the authenticated user. Safe to call with
     * a null/other-principal authentication: the audit log simply is not written in that case.
     */
    private void logAuditEvent(AuditLogService auditLogService, UserDAO userDAO,
                               Authentication authentication, AuditAction action, String ipAddress) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails details)) {
            return;
        }
        User user = userDAO.findByUsername(details.getUsername());
        if (user == null) {
            return;
        }
        auditLogService.logAction("User", user.getId(), action, user.getId(), ipAddress, null, null);
    }
}
