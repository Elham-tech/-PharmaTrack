package com.example.PharmaTrack.controller;

import com.example.PharmaTrack.entity.InventoryBatch;
import com.example.PharmaTrack.service.InventoryBatchService;
import org.springframework.security.access.prepost.PreAuthorize;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

// Marks this class as a REST controller; all methods return JSON responses
@RestController
// Base URL path for all endpoints in this controller
@RequestMapping("/api/inventory-batches")
// Allows requests from any origin (CORS) - restrict in production
@CrossOrigin(origins = "*")
// Role-based access: ADMIN and INVENTORY_MANAGER only
@PreAuthorize("hasAnyAuthority(@appAuthorities.admin, @appAuthorities.inventoryManager)")
public class InventoryBatchController {

    private final InventoryBatchService inventoryBatchService;

    @Autowired
    public InventoryBatchController(InventoryBatchService inventoryBatchService) {
        this.inventoryBatchService = inventoryBatchService;
    }

    // Handles HTTP POST requests to create a new resource
    @PostMapping
    // Deserializes the HTTP request body JSON into an InventoryBatch object
    public ResponseEntity<InventoryBatch> createInventoryBatch(@Valid @RequestBody InventoryBatch inventoryBatch) {
        InventoryBatch createdBatch = inventoryBatchService.createInventoryBatch(inventoryBatch);
        return new ResponseEntity<>(createdBatch, HttpStatus.CREATED);
    }

    // Handles HTTP PUT requests to update an existing resource by ID
    @PutMapping("/{id}")
    public ResponseEntity<InventoryBatch> updateInventoryBatch(
            @PathVariable Long id,
            @Valid @RequestBody InventoryBatch inventoryBatch) {
        InventoryBatch updatedBatch = inventoryBatchService.updateInventoryBatch(id, inventoryBatch);
        return ResponseEntity.ok(updatedBatch);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<InventoryBatch> patchInventoryBatch(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) throws com.fasterxml.jackson.databind.JsonMappingException {
        // ARCHITECTURE: Quantity is auto-managed by stock movements (stock-in/stock-out),
        // so it can never be set directly through the API.
        updates.remove("quantity");
        updates.remove("quantityRemaining");
        InventoryBatch existing = inventoryBatchService.getInventoryBatchById(id);
        new ObjectMapper().updateValue(existing, updates);
        return ResponseEntity.ok(inventoryBatchService.updateInventoryBatch(id, existing));
    }

    // Handles HTTP GET requests to retrieve a single resource by ID
    @GetMapping("/{id}")
    public ResponseEntity<InventoryBatch> getInventoryBatchById(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        InventoryBatch batch = inventoryBatchService.getInventoryBatchById(id);
        return ResponseEntity.ok(batch);
    }

    // Handles HTTP GET requests to retrieve a single resource by ID
    @GetMapping("/number/{batchNumber}")
    public ResponseEntity<InventoryBatch> getInventoryBatchByNumber(
            // Extracts the {id} value from the URL path
            @PathVariable String batchNumber) {
        InventoryBatch batch = inventoryBatchService.getInventoryBatchByNumber(batchNumber);
        return ResponseEntity.ok(batch);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping
    public ResponseEntity<List<InventoryBatch>> getAllInventoryBatches() {
        List<InventoryBatch> batches = inventoryBatchService.getAllInventoryBatches();
        return ResponseEntity.ok(batches);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/medicine/{medicineId}")
    public ResponseEntity<List<InventoryBatch>> getBatchesByMedicine(
            // Extracts the {id} value from the URL path
            @PathVariable Long medicineId) {
        List<InventoryBatch> batches = inventoryBatchService.getBatchesByMedicine(medicineId);
        return ResponseEntity.ok(batches);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<InventoryBatch>> getBatchesBySupplier(
            // Extracts the {id} value from the URL path
            @PathVariable Long supplierId) {
        List<InventoryBatch> batches = inventoryBatchService.getBatchesBySupplier(supplierId);
        return ResponseEntity.ok(batches);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/expiring")
    public ResponseEntity<List<InventoryBatch>> getExpiringBatches(
            // Extracts the value from the query parameter ?name=...
            @RequestParam
            // Tells Spring to parse the query param as an ISO-8601 date string
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate) {
        List<InventoryBatch> batches = inventoryBatchService.getExpiringBatches(expiryDate);
        return ResponseEntity.ok(batches);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/available/medicine/{medicineId}")
    public ResponseEntity<List<InventoryBatch>> getAvailableBatchesByMedicine(
            // Extracts the {id} value from the URL path
            @PathVariable Long medicineId) {
        List<InventoryBatch> batches = inventoryBatchService.getAvailableBatchesByMedicine(medicineId);
        return ResponseEntity.ok(batches);
    }

    // Handles HTTP DELETE requests to remove a resource by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInventoryBatch(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        inventoryBatchService.deleteInventoryBatch(id);
        return ResponseEntity.noContent().build();
    }
}
