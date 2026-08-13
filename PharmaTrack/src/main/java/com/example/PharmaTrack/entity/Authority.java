package com.example.PharmaTrack.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/*
 * ARCHITECTURE: A GENERIC authority/role entity used by Spring Security for
 * authentication and authorization. Authorities are stored in the database and
 * linked to users through a many-to-many join table (user_authorities). No
 * authority name is hard-coded in the security configuration - everything is
 * read dynamically from this table at runtime.
 */
// Marks this class as a JPA entity, mapped to a database table
@Entity
// Specifies the table name in the database
@Table(name = "authorities")
public class Authority {

    // Primary key field
    @Id
    // Auto-generates the ID using the database's auto-increment strategy
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Validates that the field is not blank (not null and not empty/whitespace)
    @NotBlank(message = "Authority name is required")
    // Maps the field to a specific database column with constraints
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    public Authority() {}

    public Authority(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
