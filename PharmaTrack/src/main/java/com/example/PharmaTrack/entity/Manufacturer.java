package com.example.PharmaTrack.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.List;

// Marks this class as a JPA entity, mapped to a database table
@Entity
// Specifies the table name in the database
@Table(name = "manufacturers")
public class Manufacturer {

    // Primary key field
    @Id
    // Auto-generates the ID using the database's auto-increment strategy
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Validates that the field is not blank (not null and not empty/whitespace)
    @NotBlank(message = "Manufacturer name is required")
    // Enforces a maximum character length on the field
    @Size(max = 200, message = "Manufacturer name must not exceed 200 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    // Enforces a maximum character length on the field
    @Size(max = 500, message = "Address must not exceed 500 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "address")
    private String address;

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
    @Size(max = 100, message = "Country must not exceed 100 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "country")
    private String country;

    // Prevents this field from being serialized to JSON (avoids circular reference)
    @JsonIgnore
    // Defines a one-to-many relationship (one manufacturer/category has many medicines)
    @OneToMany(mappedBy = "manufacturer", cascade = CascadeType.ALL)
    private List<Medicine> medicines;

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

    public Manufacturer() {}

    public Manufacturer(String name, String address, String phone, String email, String country) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.country = country;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public List<Medicine> getMedicines() { return medicines; }
    public void setMedicines(List<Medicine> medicines) { this.medicines = medicines; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
