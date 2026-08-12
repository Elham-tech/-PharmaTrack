package com.example.PharmaTrack.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Marks this class as a JPA entity, mapped to a database table
@Entity
// Specifies the table name in the database
@Table(name = "dispensing_records")
public class DispensingRecord {

    // Primary key field
    @Id
    // Auto-generates the ID using the database's auto-increment strategy
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Validates that the field is not blank (not null and not empty/whitespace)
    @NotBlank(message = "Dispensing number is required")
    // Maps the field to a specific database column with constraints
    @Column(name = "dispensing_number", nullable = false, unique = true)
    private String dispensingNumber;

    // Validates that the field is not null
    @NotNull(message = "Prescription is required")
    // Defines a many-to-one relationship (many medicines belong to one category/manufacturer)
    @ManyToOne
    // Specifies the foreign key column that links to the related entity's table
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

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
    @NotNull(message = "Quantity dispensed is required")
    // Validates that the numeric value is strictly positive
    @Positive(message = "Quantity dispensed must be positive")
    // Maps the field to a specific database column with constraints
    @Column(name = "quantity_dispensed", nullable = false)
    private int quantityDispensed;

    // Validates that the field is not null
    @NotNull(message = "Unit price is required")
    // Validates that the decimal value is greater than the specified minimum
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
    // Maps the field to a specific database column with constraints
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // Validates that the field is not null
    @NotNull(message = "Total price is required")
    // Validates that the decimal value is greater than the specified minimum
    @DecimalMin(value = "0.0", inclusive = false, message = "Total price must be greater than 0")
    // Maps the field to a specific database column with constraints
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    // Validates that the field is not null
    @NotNull(message = "Dispensed by user is required")
    // Defines a many-to-one relationship (many medicines belong to one category/manufacturer)
    @ManyToOne
    // Specifies the foreign key column that links to the related entity's table
    @JoinColumn(name = "dispensed_by_user_id", nullable = false)
    private User dispensedBy;

    // Validates that the field is not null
    @NotNull(message = "Payment status is required")
    // Stores the enum as a string in the database instead of an ordinal integer
    @Enumerated(EnumType.STRING)
    // Maps the field to a specific database column with constraints
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // Cashier who approved (PAID) or voided (VOIDED) the record
    @ManyToOne
    @JoinColumn(name = "processed_by_user_id")
    private User processedBy;

    // Maps the field to a specific database column with constraints
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // Maps the field to a specific database column with constraints
    @Column(name = "dispensing_date", nullable = false)
    private LocalDateTime dispensingDate;

    // Maps the field to a specific database column with constraints
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Lifecycle callback: invoked before the entity is persisted (inserted) for the first time
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (dispensingDate == null) dispensingDate = LocalDateTime.now();
    }

    /*
     * ARCHITECTURE: Payment/approval workflow state. A dispensing record is PENDING when
     * created, then a cashier either approves it (PAID) or voids it (VOIDED).
     */
    public enum PaymentStatus {
        PENDING, PAID, VOIDED
    }

    public DispensingRecord() {}

    public DispensingRecord(String dispensingNumber, Prescription prescription, Medicine medicine,
                            InventoryBatch inventoryBatch, int quantityDispensed,
                            BigDecimal unitPrice, BigDecimal totalPrice, User dispensedBy) {
        this.dispensingNumber = dispensingNumber;
        this.prescription = prescription;
        this.medicine = medicine;
        this.inventoryBatch = inventoryBatch;
        this.quantityDispensed = quantityDispensed;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.dispensedBy = dispensedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDispensingNumber() { return dispensingNumber; }
    public void setDispensingNumber(String dispensingNumber) { this.dispensingNumber = dispensingNumber; }
    public Prescription getPrescription() { return prescription; }
    public void setPrescription(Prescription prescription) { this.prescription = prescription; }
    public Medicine getMedicine() { return medicine; }
    public void setMedicine(Medicine medicine) { this.medicine = medicine; }
    public InventoryBatch getInventoryBatch() { return inventoryBatch; }
    public void setInventoryBatch(InventoryBatch inventoryBatch) { this.inventoryBatch = inventoryBatch; }
    public int getQuantityDispensed() { return quantityDispensed; }
    public void setQuantityDispensed(int quantityDispensed) { this.quantityDispensed = quantityDispensed; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public User getDispensedBy() { return dispensedBy; }
    public void setDispensedBy(User dispensedBy) { this.dispensedBy = dispensedBy; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public User getProcessedBy() { return processedBy; }
    public void setProcessedBy(User processedBy) { this.processedBy = processedBy; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public LocalDateTime getDispensingDate() { return dispensingDate; }
    public void setDispensingDate(LocalDateTime dispensingDate) { this.dispensingDate = dispensingDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
