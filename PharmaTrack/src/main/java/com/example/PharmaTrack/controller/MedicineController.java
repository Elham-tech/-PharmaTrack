/*
 * ARCHITECTURE: Controllers are thin - they ONLY handle HTTP concerns (parsing request,
 * returning response). All business logic (validation, duplicate checks, persistence) lives
 * in the service layer. This separation means controllers can be tested with mock services,
 * and the same business logic can be reused from CLI, scheduled jobs, or other entry points.
 */
package com.example.PharmaTrack.controller;

import com.example.PharmaTrack.entity.Medicine;
import com.example.PharmaTrack.service.MedicineService;
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
@RequestMapping("/api/medicines")
// Allows requests from any origin (CORS) - restrict in production
@CrossOrigin(origins = "*")
// Role-based access: ADMIN and INVENTORY_MANAGER only
@PreAuthorize("hasAnyAuthority(@appAuthorities.admin, @appAuthorities.inventoryManager)")
public class MedicineController {

    private final MedicineService medicineService;

    @Autowired
    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    // Handles HTTP POST requests to create a new resource
    @PostMapping
    // Deserializes the HTTP request body JSON into a Medicine object
    public ResponseEntity<Medicine> createMedicine(@Valid @RequestBody Medicine medicine) {
        Medicine createdMedicine = medicineService.createMedicine(medicine);
        return new ResponseEntity<>(createdMedicine, HttpStatus.CREATED);
    }

    // Handles HTTP PUT requests to update an existing resource by ID
    @PutMapping("/{id}")
    public ResponseEntity<Medicine> updateMedicine(
            @PathVariable Long id,
            @Valid @RequestBody Medicine medicine) {
        Medicine updatedMedicine = medicineService.updateMedicine(id, medicine);
        return ResponseEntity.ok(updatedMedicine);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Medicine> patchMedicine(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) throws com.fasterxml.jackson.databind.JsonMappingException {
        Medicine existing = medicineService.getMedicineById(id);
        new ObjectMapper().updateValue(existing, updates);
        return ResponseEntity.ok(medicineService.updateMedicine(id, existing));
    }

    // Handles HTTP GET requests to retrieve a single resource by ID
    @GetMapping("/{id}")
    public ResponseEntity<Medicine> getMedicineById(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        Medicine medicine = medicineService.getMedicineById(id);
        return ResponseEntity.ok(medicine);
    }

    // Handles HTTP GET requests to retrieve a single resource by ID
    @GetMapping("/code/{code}")
    public ResponseEntity<Medicine> getMedicineByCode(
            // Extracts the {id} value from the URL path
            @PathVariable String code) {
        Medicine medicine = medicineService.getMedicineByCode(code);
        return ResponseEntity.ok(medicine);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping
    public ResponseEntity<List<Medicine>> getAllMedicines() {
        List<Medicine> medicines = medicineService.getAllMedicines();
        return ResponseEntity.ok(medicines);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Medicine>> getMedicinesByCategory(
            // Extracts the {id} value from the URL path
            @PathVariable Long categoryId) {
        List<Medicine> medicines = medicineService.getMedicinesByCategory(categoryId);
        return ResponseEntity.ok(medicines);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/manufacturer/{manufacturerId}")
    public ResponseEntity<List<Medicine>> getMedicinesByManufacturer(
            // Extracts the {id} value from the URL path
            @PathVariable Long manufacturerId) {
        List<Medicine> medicines = medicineService.getMedicinesByManufacturer(manufacturerId);
        return ResponseEntity.ok(medicines);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/search")
    public ResponseEntity<List<Medicine>> searchMedicines(
            // Extracts the value from the query parameter ?name=...
            @RequestParam String name) {
        List<Medicine> medicines = medicineService.searchMedicinesByName(name);
        return ResponseEntity.ok(medicines);
    }

    // Handles HTTP DELETE requests to remove a resource by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicine(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        medicineService.deleteMedicine(id);
        return ResponseEntity.noContent().build();
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/check/code/{code}")
    public ResponseEntity<Boolean> checkCode(
            // Extracts the {id} value from the URL path
            @PathVariable String code) {
        return ResponseEntity.ok(medicineService.existsByCode(code));
    }
}
