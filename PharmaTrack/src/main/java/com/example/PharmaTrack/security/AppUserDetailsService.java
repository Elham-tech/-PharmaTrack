/*
 * ARCHITECTURE: Spring Security's UserDetailsService loads a user from the database by
 * username during authentication (form login). It converts the persisted generic
 * Authority entities into GrantedAuthority objects so the framework can evaluate
 * @PreAuthorize / hasAuthority() expressions. The user's password is a BCrypt hash,
 * and the returned UserDetails carries that hash so the AuthenticationManager can
 * verify the raw password entered at login against it.
 */
package com.example.PharmaTrack.security;

import com.example.PharmaTrack.dao.UserDAO;
import com.example.PharmaTrack.entity.Authority;
import com.example.PharmaTrack.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserDAO userDAO;

    // Constructor injection (Spring Security 6 requirement)
    public AppUserDetailsService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /*
     * Loads the user with its authorities. @Transactional keeps the persistence context
     * open so the eagerly fetched authorities remain available to the security layer.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userDAO.findByUsernameWithAuthorities(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        // Convert each database Authority into a GrantedAuthority (no hard-coded names)
        List<GrantedAuthority> grantedAuthorities = user.getAuthorities().stream()
            .map(Authority::getName)
            .map(SimpleGrantedAuthority::new)
            .map(authority -> (GrantedAuthority) authority)
            .toList();

        // Spring's User (implements UserDetails): the isActive() flag controls the
        // "enabled" status, so deactivated users are rejected at login.
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            user.isActive(),          // enabled
            true,                     // accountNonExpired
            true,                     // credentialsNonExpired
            true,                     // accountNonLocked
            grantedAuthorities
        );
    }
}
