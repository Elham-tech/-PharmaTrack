package com.example.PharmaTrack.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

// Marks this class as a JPA entity, mapped to a database table
@Entity
// Specifies the table name in the database
@Table(name = "suppliers")
public class Supplier {

    // Primary key field
    @Id
    // Auto-generates the ID using the database's auto-increment strategy
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Validates that the field is not blank (not null and not empty/whitespace)
    @NotBlank(message = "Supplier code is required")
    // Enforces a maximum character length on the field
    @Size(max = 50, message = "Supplier code must not exceed 50 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "code", nullable = false, unique = true)
    private String code;

    // Validates that the field is not blank (not null and not empty/whitespace)
    @NotBlank(message = "Supplier name is required")
    // Enforces a maximum character length on the field
    @Size(max = 200, message = "Supplier name must not exceed 200 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "name", nullable = false)
    private String name;

    // Enforces a maximum character length on the field
    @Size(max = 200, message = "Contact person must not exceed 200 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "contact_person")
    private String contactPerson;

    // Enforces a maximum character length on the field
    @Size(max = 50, message = "Phone must not exceed 50 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "phone")
    private String phone;

    // Validates that the field contains a valid email address
    @Email(message = "Email must be valid")
    // Enforces a maximum character length on the field
    @Size(max = 100, message = "Email must not exceed 100 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "email")
    private String email;

    // Enforces a maximum character length on the field
    @Size(max = 500, message = "Address must not exceed 500 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "address")
    private String address;

    // Enforces a maximum character length on the field
    @Size(max = 100, message = "City must not exceed 100 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "city")
    private String city;

    // Enforces a maximum character length on the field
    @Size(max = 100, message = "Country must not exceed 100 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "country")
    private String country;

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

    public Supplier() {}

    public Supplier(String code, String name, String contactPerson, String phone, String email, String address, String city, String country) {
        this.code = code;
        this.name = name;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.city = city;
        this.country = country;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
