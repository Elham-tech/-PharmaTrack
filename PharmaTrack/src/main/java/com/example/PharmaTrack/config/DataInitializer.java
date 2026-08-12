/*
 * ARCHITECTURE: Startup initializer that keeps the DB-driven authorization model usable:
 * 1. Seeds the authorities table from the business Role enum values (seed data, NOT
 *    hard-coded security configuration - the SecurityConfig references no names).
 * 2. Back-fills authorities for pre-existing users based on their legacy role field so
 *    they can immediately authenticate after the upgrade.
 * 3. Optionally creates a default admin account when the users table is empty, so the
 *    application is usable out of the box. Disable via app.security.seed-default-admin=false.
 */
package com.example.PharmaTrack.config;

import com.example.PharmaTrack.dao.AuthorityDAO;
import com.example.PharmaTrack.dao.UserDAO;
import com.example.PharmaTrack.entity.Authority;
import com.example.PharmaTrack.entity.Role;
import com.example.PharmaTrack.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AuthorityDAO authorityDAO;
    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.seed-default-admin:true}")
    private boolean seedDefaultAdmin;

    // Constructor injection
    public DataInitializer(AuthorityDAO authorityDAO, UserDAO userDAO, PasswordEncoder passwordEncoder) {
        this.authorityDAO = authorityDAO;
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Seed one authority row per business role (if missing)
        for (Role role : Role.values()) {
            if (authorityDAO.findByName(role.name()) == null) {
                authorityDAO.save(new Authority(role.name()));
            }
        }

        // 2. Back-fill authorities for existing users that don't have any yet
        List<User> users = userDAO.findAll();
        for (User user : users) {
            if (user.getRole() != null && (user.getAuthorities() == null || user.getAuthorities().isEmpty())) {
                Set<Authority> authorities = new HashSet<>();
                Authority authority = authorityDAO.findByName(user.getRole().name());
                if (authority != null) {
                    authorities.add(authority);
                    user.setAuthorities(authorities);
                    userDAO.save(user);
                }
            }
        }

        // 3. Seed a default admin account only when the database has no users at all
        if (seedDefaultAdmin && userDAO.findAll().isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@pharmatrack.local");
            admin.setFullName("System Administrator");
            admin.setRole(Role.ADMIN);
            Authority adminAuthority = authorityDAO.findByName(Role.ADMIN.name());
            if (adminAuthority == null) {
                adminAuthority = new Authority(Role.ADMIN.name());
                authorityDAO.save(adminAuthority);
            }
            admin.setAuthorities(new HashSet<>(Set.of(adminAuthority)));
            userDAO.save(admin);
            System.out.println("[DataInitializer] Created default admin user (admin / admin123)");
        }
    }
}
