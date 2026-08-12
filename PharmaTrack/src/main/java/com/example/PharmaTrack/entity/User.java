package com.example.PharmaTrack.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// Marks this class as a JPA entity, mapped to a database table
@Entity
// Specifies the table name in the database
@Table(name = "users")
public class User {

    // Primary key field
    @Id
    // Auto-generates the ID using the database's auto-increment strategy
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Validates that the field is not blank (not null and not empty/whitespace)
    @NotBlank(message = "Username is required")
    // Enforces a maximum character length on the field
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    // Prevents the password from being included in JSON responses (write-only)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    // Validates that the field is not blank (not null and not empty/whitespace)
    @NotBlank(message = "Password is required")
    // Enforces a maximum character length on the field
    @Size(min = 6, message = "Password must be at least 6 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "password", nullable = false)
    private String password;

    // Validates that the field is not blank (not null and not empty/whitespace)
    @NotBlank(message = "Email is required")
    // Validates that the field contains a valid email address
    @Email(message = "Email must be valid")
    // Maps the field to a specific database column with constraints
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    // Validates that the field is not blank (not null and not empty/whitespace)
    @NotBlank(message = "Full name is required")
    // Enforces a maximum character length on the field
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "full_name", nullable = false)
    private String fullName;

    /*
     * ARCHITECTURE: Business-level role used by the pharmacy UI (kept for
     * backward compatibility). It is NOT used by Spring Security - authentication
     * and authorization rely exclusively on the generic authorities set below,
     * which is read from the database.
     */
    // Stores the enum as a string in the database instead of an ordinal integer
    @Enumerated(EnumType.STRING)
    // Nullable so users registered with generic (non-pharmacy) authority names can exist
    @Column(name = "role")
    private Role role;

    /*
     * ARCHITECTURE: Many-to-many relationship between users and generic authorities
     * (roles). This is the source of truth for Spring Security: the UserDetailsService
     * reads these authorities from the database and converts them into GrantedAuthority
     * objects. Lazy loading keeps user listings fast; the UserDetailsService loads them
     * eagerly via a JOIN FETCH query inside a transaction.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    // Specifies the join table that links users to authorities
    @JoinTable(
        name = "user_authorities",
        // Foreign key column pointing back to this entity's table
        joinColumns = @JoinColumn(name = "user_id"),
        // Foreign key column pointing to the related entity's table
        inverseJoinColumns = @JoinColumn(name = "authority_id"))
    private Set<Authority> authorities = new HashSet<>();

    // Maps the field to a specific database column with constraints
    @Column(name = "active", nullable = false)
    private boolean active = true;

    // Maps the field to a specific database column with constraints
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Maps the field to a specific database column with constraints
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Lifecycle callback: invoked before the entity is persisted (inserted) for the first time
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // Lifecycle callback: invoked every time the entity is updated
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public User() {}

    public User(String username, String password, String email, String fullName, Role role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Set<Authority> getAuthorities() { return authorities; }
    public void setAuthorities(Set<Authority> authorities) { this.authorities = authorities; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
