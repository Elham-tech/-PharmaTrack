package com.example.PharmaTrack.controller;

import com.example.PharmaTrack.entity.Supplier;
import com.example.PharmaTrack.service.SupplierService;
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
@RequestMapping("/api/suppliers")
// Allows requests from any origin (CORS) - restrict in production
@CrossOrigin(origins = "*")
// Role-based access: ADMIN and INVENTORY_MANAGER only
@PreAuthorize("hasAnyAuthority(@appAuthorities.admin, @appAuthorities.inventoryManager)")
public class SupplierController {

    private final SupplierService supplierService;

    @Autowired
    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    // Handles HTTP POST requests to create a new resource
    @PostMapping
    // Deserializes the HTTP request body JSON into a Supplier object
    public ResponseEntity<Supplier> createSupplier(@Valid @RequestBody Supplier supplier) {
        Supplier createdSupplier = supplierService.createSupplier(supplier);
        return new ResponseEntity<>(createdSupplier, HttpStatus.CREATED);
    }

    // Handles HTTP PUT requests to update an existing resource by ID
    @PutMapping("/{id}")
    public ResponseEntity<Supplier> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody Supplier supplier) {
        Supplier updatedSupplier = supplierService.updateSupplier(id, supplier);
        return ResponseEntity.ok(updatedSupplier);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Supplier> patchSupplier(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) throws com.fasterxml.jackson.databind.JsonMappingException {
        Supplier existing = supplierService.getSupplierById(id);
        new ObjectMapper().updateValue(existing, updates);
        return ResponseEntity.ok(supplierService.updateSupplier(id, existing));
    }

    // Handles HTTP GET requests to retrieve a single resource by ID
    @GetMapping("/{id}")
    public ResponseEntity<Supplier> getSupplierById(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        Supplier supplier = supplierService.getSupplierById(id);
        return ResponseEntity.ok(supplier);
    }

    // Handles HTTP GET requests to retrieve a single resource by ID
    @GetMapping("/code/{code}")
    public ResponseEntity<Supplier> getSupplierByCode(
            // Extracts the {id} value from the URL path
            @PathVariable String code) {
        Supplier supplier = supplierService.getSupplierByCode(code);
        return ResponseEntity.ok(supplier);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping
    public ResponseEntity<List<Supplier>> getAllSuppliers() {
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        return ResponseEntity.ok(suppliers);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/active")
    public ResponseEntity<List<Supplier>> getActiveSuppliers() {
        List<Supplier> suppliers = supplierService.getActiveSuppliers();
        return ResponseEntity.ok(suppliers);
    }

    // Handles HTTP DELETE requests to remove a resource by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/check/code/{code}")
    public ResponseEntity<Boolean> checkCode(
            // Extracts the {id} value from the URL path
            @PathVariable String code) {
        return ResponseEntity.ok(supplierService.existsByCode(code));
    }
}
