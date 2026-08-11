package com.example.PharmaTrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Marks this class as a JPA entity, mapped to a database table
@Entity
// Specifies the table name in the database
@Table(name = "prescriptions")
public class Prescription {

    // Primary key field
    @Id
    // Auto-generates the ID using the database's auto-increment strategy
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Validates that the field is not blank (not null and not empty/whitespace)
    @NotBlank(message = "Prescription number is required")
    // Maps the field to a specific database column with constraints
    @Column(name = "prescription_number", nullable = false, unique = true)
    private String prescriptionNumber;

    // Validates that the field is not blank (not null and not empty/whitespace)
    @NotBlank(message = "Patient name is required")
    // Enforces a maximum character length on the field
    @Size(max = 200, message = "Patient name must not exceed 200 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "patient_name", nullable = false)
    private String patientName;

    // Enforces a maximum character length on the field
    @Size(max = 50, message = "Patient ID number must not exceed 50 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "patient_id_number")
    private String patientIdNumber;

    // Enforces a maximum character length on the field
    @Size(max = 200, message = "Doctor name must not exceed 200 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "doctor_name")
    private String doctorName;

    // Enforces a maximum character length on the field
    @Size(max = 200, message = "Hospital name must not exceed 200 characters")
    // Maps the field to a specific database column with constraints
    @Column(name = "hospital_name")
    private String hospitalName;

    // Maps the field to a specific database column with constraints
    @Column(name = "prescription_details", columnDefinition = "TEXT")
    private String prescriptionDetails;

    // Maps the field to a specific database column with constraints
    @Column(name = "dispensed", nullable = false)
    private boolean dispensed = false;

    // Maps the field to a specific database column with constraints
    @Column(name = "voided", nullable = false)
    private boolean voided = false;

    // Maps the field to a specific database column with constraints
    @Column(name = "dispensed_date")
    private LocalDateTime dispensedDate;

    // Defines a many-to-one relationship (many medicines belong to one category/manufacturer)
    @ManyToOne
    // Specifies the foreign key column that links to the related entity's table
    @JoinColumn(name = "dispensed_by_user_id")
    private User dispensedBy;

    @JsonIgnoreProperties("prescription")
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PrescriptionItem> items = new ArrayList<>();

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

    public Prescription() {}

    public Prescription(String prescriptionNumber, String patientName, String doctorName,
                        String prescriptionDetails) {
        this.prescriptionNumber = prescriptionNumber;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.prescriptionDetails = prescriptionDetails;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPrescriptionNumber() { return prescriptionNumber; }
    public void setPrescriptionNumber(String prescriptionNumber) { this.prescriptionNumber = prescriptionNumber; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getPatientIdNumber() { return patientIdNumber; }
    public void setPatientIdNumber(String patientIdNumber) { this.patientIdNumber = patientIdNumber; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
    public String getPrescriptionDetails() { return prescriptionDetails; }
    public void setPrescriptionDetails(String prescriptionDetails) { this.prescriptionDetails = prescriptionDetails; }
    public boolean isDispensed() { return dispensed; }
    public void setDispensed(boolean dispensed) { this.dispensed = dispensed; }
    public boolean isVoided() { return voided; }
    public void setVoided(boolean voided) { this.voided = voided; }
    public LocalDateTime getDispensedDate() { return dispensedDate; }
    public void setDispensedDate(LocalDateTime dispensedDate) { this.dispensedDate = dispensedDate; }
    public User getDispensedBy() { return dispensedBy; }
    public void setDispensedBy(User dispensedBy) { this.dispensedBy = dispensedBy; }
    public List<PrescriptionItem> getItems() { return items; }
    public void setItems(List<PrescriptionItem> items) { this.items = items; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
