package com.example.PharmaTrack.controller;

import com.example.PharmaTrack.dto.StockMovementRequest;
import com.example.PharmaTrack.entity.StockMovement;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.PharmaTrack.entity.StockMovement.MovementType;
import com.example.PharmaTrack.service.StockMovementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.Valid;

// Marks this class as a REST controller; all methods return JSON responses
@RestController
// Base URL path for all endpoints in this controller
@RequestMapping("/api/stock-movements")
// Allows requests from any origin (CORS) - restrict in production
@CrossOrigin(origins = "*")
// Role-based access: ADMIN and INVENTORY_MANAGER only
@PreAuthorize("hasAnyAuthority(@appAuthorities.admin, @appAuthorities.inventoryManager)")
public class StockMovementController {

    private final StockMovementService stockMovementService;

    @Autowired
    public StockMovementController(StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    // Handles HTTP POST requests to create a new resource
    @PostMapping
    // Deserializes the HTTP request body JSON into a StockMovement object
    public ResponseEntity<StockMovement> createStockMovement(@Valid @RequestBody StockMovement stockMovement) {
        StockMovement createdMovement = stockMovementService.createStockMovement(stockMovement);
        return new ResponseEntity<>(createdMovement, HttpStatus.CREATED);
    }

    // Handles HTTP GET requests to retrieve a single resource by ID
    @GetMapping("/{id}")
    public ResponseEntity<StockMovement> getStockMovementById(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        StockMovement movement = stockMovementService.getStockMovementById(id);
        return ResponseEntity.ok(movement);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping
    public ResponseEntity<List<StockMovement>> getAllStockMovements() {
        List<StockMovement> movements = stockMovementService.getAllStockMovements();
        return ResponseEntity.ok(movements);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/type/{movementType}")
    public ResponseEntity<List<StockMovement>> getStockMovementsByType(
            // Extracts the {id} value from the URL path
            @PathVariable MovementType movementType) {
        List<StockMovement> movements = stockMovementService.getStockMovementsByType(movementType);
        return ResponseEntity.ok(movements);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/medicine/{medicineId}")
    public ResponseEntity<List<StockMovement>> getStockMovementsByMedicine(
            // Extracts the {id} value from the URL path
            @PathVariable Long medicineId) {
        List<StockMovement> movements = stockMovementService.getStockMovementsByMedicine(medicineId);
        return ResponseEntity.ok(movements);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/batch/{batchId}")
    public ResponseEntity<List<StockMovement>> getStockMovementsByBatch(
            // Extracts the {id} value from the URL path
            @PathVariable Long batchId) {
        List<StockMovement> movements = stockMovementService.getStockMovementsByBatch(batchId);
        return ResponseEntity.ok(movements);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<StockMovement>> getStockMovementsByUser(
            // Extracts the {id} value from the URL path
            @PathVariable Long userId) {
        List<StockMovement> movements = stockMovementService.getStockMovementsByUser(userId);
        return ResponseEntity.ok(movements);
    }

    // Handles HTTP POST requests to process a stock-in
    @PostMapping("/stock-in")
    public ResponseEntity<Void> processStockIn(@Valid @RequestBody StockMovementRequest request) {
        stockMovementService.processStockIn(request);
        return ResponseEntity.ok().build();
    }

    // Handles HTTP POST requests to process a stock-out
    @PostMapping("/stock-out")
    public ResponseEntity<Void> processStockOut(@Valid @RequestBody StockMovementRequest request) {
        stockMovementService.processStockOut(request);
        return ResponseEntity.ok().build();
    }
}
