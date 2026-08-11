/*
 * ARCHITECTURE: JPA entity class maps to a database table. Validation annotations (@NotBlank,
 * @Size, @DecimalMin) are placed here so that @Valid on controller @RequestBody automatically
 * enforces data integrity at the API layer BEFORE the service or DAO is even called. This
 * follows the "validate early" principle - bad data is rejected at the boundary.
 */
package com.example.PharmaTrack.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

// Marks this class as a JPA entity, mapped to a database table
@Entity
// Specifies the table name in the database
@Table(name = "medicines")
public class Medicine {

    // Primary key field
    @Id
    // Auto-generates the ID using the database's auto-increment strategy
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Validates that the field is not blank (not null and not empty/whitespace)
    @NotBlank(message = "Medicine code is required")
    // Enforces a maximum character length on the field
    @Size(max = 50, message = "Medicine code must not exceed 50 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "code", nullable = false, unique = true)
    private String code;

    // Validates that the field is not blank (not null and not empty/whitespace)
    @NotBlank(message = "Medicine name is required")
    // Enforces a maximum character length on the field
    @Size(max = 200, message = "Medicine name must not exceed 200 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "name", nullable = false)
    private String name;

    // Enforces a maximum character length on the field
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "description")
    private String description;

    // Validates that the field is not null
    @NotNull(message = "Category is required")
    // Defines a many-to-one relationship (many medicines belong to one category/manufacturer)
    @ManyToOne
    // Specifies the foreign key column that links to the related entity's table
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Validates that the field is not null
    @NotNull(message = "Manufacturer is required")
    // Defines a many-to-one relationship (many medicines belong to one category/manufacturer)
    @ManyToOne
    // Specifies the foreign key column that links to the related entity's table
    @JoinColumn(name = "manufacturer_id", nullable = false)
    private Manufacturer manufacturer;

    // Validates that the field is not blank (not null and not empty/whitespace)
    @NotBlank(message = "Unit is required")
    // Enforces a maximum character length on the field
    @Size(max = 50, message = "Unit must not exceed 50 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "unit", nullable = false)
    private String unit;

    // Maps the field to a specific database column with constraints
    @Column(name = "requires_prescription", nullable = false)
    private boolean requiresPrescription = false;

    // Maps the field to a specific database column with constraints
    @Column(name = "active", nullable = false)
    private boolean active = true;

    // Maps the field to a specific database column with constraints
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Maps the field to a specific database column with constraints
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /*
     * ARCHITECTURE: JPA lifecycle callbacks automatically set timestamps without requiring
     * the service layer to remember. @PrePersist runs before INSERT, @PreUpdate runs before
     * UPDATE. This keeps timestamp logic in one place and prevents bugs where forgets to
     * set updatedAt on edits.
     */
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

    public Medicine() {}

    public Medicine(String code, String name, String description, Category category,
                   Manufacturer manufacturer, String unit,
                   boolean requiresPrescription) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.category = category;
        this.manufacturer = manufacturer;
        this.unit = unit;
        this.requiresPrescription = requiresPrescription;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Manufacturer getManufacturer() { return manufacturer; }
    public void setManufacturer(Manufacturer manufacturer) { this.manufacturer = manufacturer; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public boolean isRequiresPrescription() { return requiresPrescription; }
    public void setRequiresPrescription(boolean requiresPrescription) { this.requiresPrescription = requiresPrescription; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
