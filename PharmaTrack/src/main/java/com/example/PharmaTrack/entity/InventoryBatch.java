package com.example.PharmaTrack.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Marks this class as a JPA entity, mapped to a database table
@Entity
// Specifies the table name in the database
@Table(name = "inventory_batches")
public class InventoryBatch {

    // Primary key field
    @Id
    // Auto-generates the ID using the database's auto-increment strategy
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Validates that the field is not blank (not null and not empty/whitespace)
    @NotBlank(message = "Batch number is required")
    // Enforces a maximum character length on the field
    @Size(max = 50, message = "Batch number must not exceed 50 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "batch_number", nullable = false, unique = true)
    private String batchNumber;

    // Validates that the field is not null
    @NotNull(message = "Medicine is required")
    // Defines a many-to-one relationship (many medicines belong to one category/manufacturer)
    @ManyToOne
    // Specifies the foreign key column that links to the related entity's table
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    // Validates that the field is not null
    @NotNull(message = "Supplier is required")
    // Defines a many-to-one relationship (many medicines belong to one category/manufacturer)
    @ManyToOne
    // Specifies the foreign key column that links to the related entity's table
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    /*
     * ARCHITECTURE: Quantity is auto-managed by stock movements (stock-in adds, stock-out
     * subtracts). It starts at 0 when a batch is created and is never entered directly, so
     * only a non-negative constraint is enforced here.
     */
    // Validates that the field is not null
    @NotNull(message = "Quantity is required")
    // Validates that the numeric value is greater than or equal to the specified minimum
    @Min(value = 0, message = "Quantity cannot be negative")
    // Maps the field to a specific database column with constraints
    @Column(name = "quantity", nullable = false)
    private int quantity;

    // Validates that the field is not null
    @NotNull(message = "Quantity remaining is required")
    // Validates that the numeric value is greater than or equal to the specified minimum
    @Min(value = 0, message = "Quantity remaining cannot be negative")
    // Maps the field to a specific database column with constraints
    @Column(name = "quantity_remaining", nullable = false)
    private int quantityRemaining;

    // Validates that the field is not null
    @NotNull(message = "Unit cost is required")
    // Validates that the decimal value is greater than the specified minimum
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit cost must be greater than 0")
    // Maps the field to a specific database column with constraints
    @Column(name = "unit_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost;

    // Selling price per unit, defined at the batch level
    // Validates that the field is not null
    @NotNull(message = "Unit price is required")
    // Validates that the decimal value is greater than the specified minimum
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
    // Maps the field to a specific database column with constraints
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // Validates that the field is not null
    @NotNull(message = "Manufacturing date is required")
    // Validates that the date is in the past or present
    @PastOrPresent(message = "Manufacturing date cannot be in the future")
    // Maps the field to a specific database column with constraints
    @Column(name = "manufacturing_date", nullable = false)
    private LocalDate manufacturingDate;

    // Validates that the field is not null
    @NotNull(message = "Expiry date is required")
    // Validates that the date is in the future
    @Future(message = "Expiry date must be in the future")
    // Maps the field to a specific database column with constraints
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    // Maps the field to a specific database column with constraints
    @Column(name = "expired", nullable = false)
    private boolean expired = false;

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

    public InventoryBatch() {}

    public InventoryBatch(String batchNumber, Medicine medicine, Supplier supplier, int quantity,
                          BigDecimal unitCost, BigDecimal unitPrice, LocalDate manufacturingDate, LocalDate expiryDate) {
        this.batchNumber = batchNumber;
        this.medicine = medicine;
        this.supplier = supplier;
        this.quantity = quantity;
        this.quantityRemaining = quantity;
        this.unitCost = unitCost;
        this.unitPrice = unitPrice;
        this.manufacturingDate = manufacturingDate;
        this.expiryDate = expiryDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public Medicine getMedicine() { return medicine; }
    public void setMedicine(Medicine medicine) { this.medicine = medicine; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getQuantityRemaining() { return quantityRemaining; }
    public void setQuantityRemaining(int quantityRemaining) { this.quantityRemaining = quantityRemaining; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public void setManufacturingDate(LocalDate manufacturingDate) { this.manufacturingDate = manufacturingDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public boolean isExpired() { return expired; }
    public void setExpired(boolean expired) { this.expired = expired; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
