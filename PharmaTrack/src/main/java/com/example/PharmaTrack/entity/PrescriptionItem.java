package com.example.PharmaTrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescription_items")
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Medicine is required")
    @ManyToOne
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @NotNull(message = "Quantity per dose is required")
    @Positive(message = "Quantity per dose must be positive")
    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Size(max = 100, message = "Dosage must not exceed 100 characters")
    @Column(name = "dosage")
    private String dosage;

    @Min(value = 1, message = "Times per day must be at least 1")
    @Column(name = "times_per_day", nullable = false)
    private int timesPerDay = 1;

    @Min(value = 1, message = "Duration days must be at least 1")
    @Column(name = "duration_days", nullable = false)
    private int durationDays = 1;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Column(name = "notes")
    private String notes;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public PrescriptionItem() {}

    public int getTotalQuantity() {
        return quantity * timesPerDay * durationDays;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Medicine getMedicine() { return medicine; }
    public void setMedicine(Medicine medicine) { this.medicine = medicine; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public int getTimesPerDay() { return timesPerDay; }
    public void setTimesPerDay(int timesPerDay) { this.timesPerDay = timesPerDay; }
    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Prescription getPrescription() { return prescription; }
    public void setPrescription(Prescription prescription) { this.prescription = prescription; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
