package com.example.PharmaTrack.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

// Marks this class as a JPA entity, mapped to a database table
@Entity
// Specifies the table name in the database
@Table(name = "stock_movements")
public class StockMovement {

    // Primary key field
    @Id
    // Auto-generates the ID using the database's auto-increment strategy
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Validates that the field is not null
    @NotNull(message = "Movement type is required")
    // Stores the enum as a string in the database instead of an ordinal integer
    @Enumerated(EnumType.STRING)
    // Maps the field to a specific database column with constraints
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType;

    // Validates that the field is not null
    @NotNull(message = "Medicine is required")
    // Defines a many-to-one relationship (many medicines belong to one category/manufacturer)
    @ManyToOne
    // Specifies the foreign key column that links to the related entity's table
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    // Validates that the field is not null
    @NotNull(message = "Inventory batch is required")
    // Defines a many-to-one relationship (many medicines belong to one category/manufacturer)
    @ManyToOne
    // Specifies the foreign key column that links to the related entity's table
    @JoinColumn(name = "inventory_batch_id", nullable = false)
    private InventoryBatch inventoryBatch;

    // Validates that the field is not null
    @NotNull(message = "Quantity is required")
    // Validates that the numeric value is strictly positive
    @Positive(message = "Quantity must be positive")
    // Maps the field to a specific database column with constraints
    @Column(name = "quantity", nullable = false)
    private int quantity;

    // Validates that the field is not null
    @NotNull(message = "Performed by user is required")
    // Defines a many-to-one relationship (many medicines belong to one category/manufacturer)
    @ManyToOne
    // Specifies the foreign key column that links to the related entity's table
    @JoinColumn(name = "performed_by_user_id", nullable = false)
    private User performedBy;

    // Enforces a maximum character length on the field
    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "reference_number")
    private String referenceNumber;

    // Enforces a maximum character length on the field
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "notes")
    private String notes;

    // Maps the field to a specific database column with constraints
    @Column(name = "movement_date", nullable = false)
    private LocalDateTime movementDate;

    // Maps the field to a specific database column with constraints
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Lifecycle callback: invoked before the entity is persisted (inserted) for the first time
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (movementDate == null) movementDate = LocalDateTime.now();
    }

    public StockMovement() {}

    public StockMovement(MovementType movementType, Medicine medicine, InventoryBatch inventoryBatch,
                         int quantity, User performedBy) {
        this.movementType = movementType;
        this.medicine = medicine;
        this.inventoryBatch = inventoryBatch;
        this.quantity = quantity;
        this.performedBy = performedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MovementType getMovementType() { return movementType; }
    public void setMovementType(MovementType movementType) { this.movementType = movementType; }
    public Medicine getMedicine() { return medicine; }
    public void setMedicine(Medicine medicine) { this.medicine = medicine; }
    public InventoryBatch getInventoryBatch() { return inventoryBatch; }
    public void setInventoryBatch(InventoryBatch inventoryBatch) { this.inventoryBatch = inventoryBatch; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public User getPerformedBy() { return performedBy; }
    public void setPerformedBy(User performedBy) { this.performedBy = performedBy; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getMovementDate() { return movementDate; }
    public void setMovementDate(LocalDateTime movementDate) { this.movementDate = movementDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public enum MovementType {
        STOCK_IN, STOCK_OUT, ADJUSTMENT, RETURN, EXPIRED_REMOVAL
    }
}
