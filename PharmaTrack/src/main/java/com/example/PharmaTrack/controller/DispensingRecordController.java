package com.example.PharmaTrack.controller;

import com.example.PharmaTrack.entity.DispensingRecord;
import com.example.PharmaTrack.service.DispensingRecordService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.Valid;

// Marks this class as a REST controller; all methods return JSON responses
@RestController
// Base URL path for all endpoints in this controller
@RequestMapping("/api/dispensing-records")
// Allows requests from any origin (CORS) - restrict in production
@CrossOrigin(origins = "*")
// Role-based access: ADMIN and CASHIER only
@PreAuthorize("hasAnyAuthority(@appAuthorities.admin, @appAuthorities.cashier)")
public class DispensingRecordController {

    private final DispensingRecordService dispensingRecordService;

    @Autowired
    public DispensingRecordController(DispensingRecordService dispensingRecordService) {
        this.dispensingRecordService = dispensingRecordService;
    }

    // Handles HTTP POST requests to create a new resource
    @PostMapping
    // Deserializes the HTTP request body JSON into a DispensingRecord object
    public ResponseEntity<DispensingRecord> createDispensingRecord(@Valid @RequestBody DispensingRecord dispensingRecord) {
        DispensingRecord createdRecord = dispensingRecordService.createDispensingRecord(dispensingRecord);
        return new ResponseEntity<>(createdRecord, HttpStatus.CREATED);
    }

    // Handles HTTP GET requests to retrieve a single resource by ID
    @GetMapping("/{id}")
    public ResponseEntity<DispensingRecord> getDispensingRecordById(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        DispensingRecord record = dispensingRecordService.getDispensingRecordById(id);
        return ResponseEntity.ok(record);
    }

    // Handles HTTP GET requests to retrieve a single resource by ID
    @GetMapping("/number/{dispensingNumber}")
    public ResponseEntity<DispensingRecord> getDispensingRecordByNumber(
            // Extracts the {id} value from the URL path
            @PathVariable String dispensingNumber) {
        DispensingRecord record = dispensingRecordService.getDispensingRecordByNumber(dispensingNumber);
        return ResponseEntity.ok(record);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping
    public ResponseEntity<List<DispensingRecord>> getAllDispensingRecords() {
        List<DispensingRecord> records = dispensingRecordService.getAllDispensingRecords();
        return ResponseEntity.ok(records);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/prescription/{prescriptionId}")
    public ResponseEntity<List<DispensingRecord>> getDispensingRecordsByPrescription(
            // Extracts the {id} value from the URL path
            @PathVariable Long prescriptionId) {
        List<DispensingRecord> records = dispensingRecordService.getDispensingRecordsByPrescription(prescriptionId);
        return ResponseEntity.ok(records);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/medicine/{medicineId}")
    public ResponseEntity<List<DispensingRecord>> getDispensingRecordsByMedicine(
            // Extracts the {id} value from the URL path
            @PathVariable Long medicineId) {
        List<DispensingRecord> records = dispensingRecordService.getDispensingRecordsByMedicine(medicineId);
        return ResponseEntity.ok(records);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DispensingRecord>> getDispensingRecordsByUser(
            // Extracts the {id} value from the URL path
            @PathVariable Long userId) {
        List<DispensingRecord> records = dispensingRecordService.getDispensingRecordsByUser(userId);
        return ResponseEntity.ok(records);
    }

    // Handles HTTP POST requests to approve a dispensing record (cashier: payment processed)
    // The approving user is always the currently logged-in user - never supplied by the client.
    @PostMapping("/{id}/approve")
    public ResponseEntity<DispensingRecord> approvePayment(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        DispensingRecord record = dispensingRecordService.approvePayment(id);
        return ResponseEntity.ok(record);
    }

    // Handles HTTP POST requests to void a dispensing record (cashier: not paid)
    // The voiding user is always the currently logged-in user - never supplied by the client.
    @PostMapping("/{id}/void")
    public ResponseEntity<DispensingRecord> voidDispensing(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        DispensingRecord record = dispensingRecordService.voidDispensing(id);
        return ResponseEntity.ok(record);
    }
}
