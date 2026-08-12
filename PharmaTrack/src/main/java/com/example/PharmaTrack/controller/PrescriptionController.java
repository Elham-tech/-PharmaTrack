package com.example.PharmaTrack.controller;

import com.example.PharmaTrack.entity.Prescription;
import com.example.PharmaTrack.service.PrescriptionService;
import org.springframework.security.access.prepost.PreAuthorize;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

// Marks this class as a REST controller; all methods return JSON responses
@RestController
// Base URL path for all endpoints in this controller
@RequestMapping("/api/prescriptions")
// Allows requests from any origin (CORS) - restrict in production
@CrossOrigin(origins = "*")
// Role-based access: ADMIN and PHARMACIST only
@PreAuthorize("hasAnyAuthority(@appAuthorities.admin, @appAuthorities.pharmacist)")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @Autowired
    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    // Handles HTTP POST requests to create a new resource
    @PostMapping
    // Deserializes the HTTP request body JSON into a Prescription object
    public ResponseEntity<Prescription> createPrescription(@Valid @RequestBody Prescription prescription) {
        Prescription createdPrescription = prescriptionService.createPrescription(prescription);
        return new ResponseEntity<>(createdPrescription, HttpStatus.CREATED);
    }

    // Handles HTTP PUT requests to update an existing resource by ID
    @PutMapping("/{id}")
    public ResponseEntity<Prescription> updatePrescription(
            @PathVariable Long id,
            @Valid @RequestBody Prescription prescription) {
        Prescription updatedPrescription = prescriptionService.updatePrescription(id, prescription);
        return ResponseEntity.ok(updatedPrescription);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Prescription> patchPrescription(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) throws com.fasterxml.jackson.databind.JsonMappingException {
        Prescription existing = prescriptionService.getPrescriptionById(id);
        new ObjectMapper().updateValue(existing, updates);
        return ResponseEntity.ok(prescriptionService.updatePrescription(id, existing));
    }

    // Handles HTTP GET requests to retrieve a single resource by ID
    @GetMapping("/{id}")
    public ResponseEntity<Prescription> getPrescriptionById(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        Prescription prescription = prescriptionService.getPrescriptionById(id);
        return ResponseEntity.ok(prescription);
    }

    // Handles HTTP GET requests to retrieve a single resource by ID
    @GetMapping("/number/{prescriptionNumber}")
    public ResponseEntity<Prescription> getPrescriptionByNumber(
            // Extracts the {id} value from the URL path
            @PathVariable String prescriptionNumber) {
        Prescription prescription = prescriptionService.getPrescriptionByNumber(prescriptionNumber);
        return ResponseEntity.ok(prescription);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping
    public ResponseEntity<List<Prescription>> getAllPrescriptions() {
        List<Prescription> prescriptions = prescriptionService.getAllPrescriptions();
        return ResponseEntity.ok(prescriptions);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/patient/{patientName}")
    public ResponseEntity<List<Prescription>> getPrescriptionsByPatient(
            // Extracts the {id} value from the URL path
            @PathVariable String patientName) {
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsByPatient(patientName);
        return ResponseEntity.ok(prescriptions);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/doctor/{doctorName}")
    public ResponseEntity<List<Prescription>> getPrescriptionsByDoctor(
            // Extracts the {id} value from the URL path
            @PathVariable String doctorName) {
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsByDoctor(doctorName);
        return ResponseEntity.ok(prescriptions);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/un-dispensed")
    public ResponseEntity<List<Prescription>> getUnDispensedPrescriptions() {
        List<Prescription> prescriptions = prescriptionService.getUnDispensedPrescriptions();
        return ResponseEntity.ok(prescriptions);
    }

    // Handles HTTP PATCH requests to mark a prescription as dispensed
    // The dispensing user is always the currently logged-in user - never supplied by the client.
    @PatchMapping("/{id}/dispense")
    public ResponseEntity<Prescription> markAsDispensed(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        Prescription prescription = prescriptionService.markAsDispensed(id);
        return ResponseEntity.ok(prescription);
    }

    // Handles HTTP DELETE requests to remove a resource by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrescription(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        prescriptionService.deletePrescription(id);
        return ResponseEntity.noContent().build();
    }
}
