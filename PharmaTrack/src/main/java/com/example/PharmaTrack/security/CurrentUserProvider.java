/*
 * ARCHITECTURE: Resolves the currently authenticated user and the caller's IP address
 * from the request context. The service layer uses this helper to record audit trail
 * entries (who performed the operation, from which IP) without every service having to
 * duplicate SecurityContextHolder / RequestContextHolder boilerplate.
 */
package com.example.PharmaTrack.security;

import com.example.PharmaTrack.dao.UserDAO;
import com.example.PharmaTrack.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class CurrentUserProvider {

    private final UserDAO userDAO;

    public CurrentUserProvider(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /*
     * Returns the User entity of the currently authenticated principal, or null when
     * there is no authenticated user (e.g. internal/system-triggered operations).
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (!(authentication.getPrincipal() instanceof UserDetails details)) {
            return null;
        }
        return userDAO.findByUsername(details.getUsername());
    }

    /*
     * Returns the id of the currently authenticated user, or null when unauthenticated.
     */
    public Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /*
     * Returns the remote IP address of the current request, or "unknown" when there is
     * no active HTTP request (e.g. commands running outside a web request).
     */
    public String getClientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getRemoteAddr();
        }
        return "unknown";
    }
}
