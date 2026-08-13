package com.example.PharmaTrack.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

// Marks this class as a JPA entity, mapped to a database table
@Entity
// Specifies the table name in the database
@Table(name = "audit_logs")
public class AuditLog {

    // Primary key field
    @Id
    // Auto-generates the ID using the database's auto-increment strategy
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Validates that the field is not blank (not null and not empty/whitespace)
    @NotBlank(message = "Entity type is required")
    // Enforces a maximum character length on the field
    @Size(max = 100, message = "Entity type must not exceed 100 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "entity_type", nullable = false)
    private String entityType;

    // Validates that the field is not null
    @NotNull(message = "Entity ID is required")
    // Maps the field to a specific database column with constraints
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    // Validates that the field is not null
    @NotNull(message = "Audit action is required")
    // Stores the enum as a string in the database instead of an ordinal integer
    @Enumerated(EnumType.STRING)
    // Maps the field to a specific database column with constraints
    @Column(name = "action", nullable = false)
    private AuditAction action;

    // Stores old values as TEXT for large audit trails
    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    // Stores old values as TEXT for large audit trails
    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    // Validates that the field is not null
    @NotNull(message = "Performed by user is required")
    // Defines a many-to-one relationship (many medicines belong to one category/manufacturer)
    @ManyToOne
    // Specifies the foreign key column that links to the related entity's table
    @JoinColumn(name = "performed_by_user_id", nullable = false)
    private User performedBy;

    // Enforces a maximum character length on the field
    @Size(max = 50, message = "IP address must not exceed 50 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    // Maps the field to a specific database column with constraints
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    // Lifecycle callback: sets timestamp before persisting
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }

    public AuditLog() {}

    public AuditLog(String entityType, Long entityId, AuditAction action, User performedBy, String ipAddress) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.performedBy = performedBy;
        this.ipAddress = ipAddress;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }
    public String getOldValues() { return oldValues; }
    public void setOldValues(String oldValues) { this.oldValues = oldValues; }
    public String getNewValues() { return newValues; }
    public void setNewValues(String newValues) { this.newValues = newValues; }
    public User getPerformedBy() { return performedBy; }
    public void setPerformedBy(User performedBy) { this.performedBy = performedBy; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public enum AuditAction {
        CREATE, UPDATE, DELETE, LOGIN, LOGOUT, DISPENSE, STOCK_IN, STOCK_OUT
    }
}
