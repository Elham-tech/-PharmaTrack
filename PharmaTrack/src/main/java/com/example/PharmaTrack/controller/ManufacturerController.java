package com.example.PharmaTrack.controller;

import com.example.PharmaTrack.entity.Manufacturer;
import com.example.PharmaTrack.service.ManufacturerService;
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
@RequestMapping("/api/manufacturers")
// Allows requests from any origin (CORS) - restrict in production
@CrossOrigin(origins = "*")
// Role-based access: ADMIN and INVENTORY_MANAGER only
@PreAuthorize("hasAnyAuthority(@appAuthorities.admin, @appAuthorities.inventoryManager)")
public class ManufacturerController {

    private final ManufacturerService manufacturerService;

    @Autowired
    public ManufacturerController(ManufacturerService manufacturerService) {
        this.manufacturerService = manufacturerService;
    }

    // Handles HTTP POST requests to create a new resource
    @PostMapping
    // Deserializes the HTTP request body JSON into a Manufacturer object
    public ResponseEntity<Manufacturer> createManufacturer(@Valid @RequestBody Manufacturer manufacturer) {
        Manufacturer createdManufacturer = manufacturerService.createManufacturer(manufacturer);
        return new ResponseEntity<>(createdManufacturer, HttpStatus.CREATED);
    }

    // Handles HTTP PUT requests to update an existing resource by ID
    @PutMapping("/{id}")
    public ResponseEntity<Manufacturer> updateManufacturer(
            @PathVariable Long id,
            @Valid @RequestBody Manufacturer manufacturer) {
        Manufacturer updatedManufacturer = manufacturerService.updateManufacturer(id, manufacturer);
        return ResponseEntity.ok(updatedManufacturer);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Manufacturer> patchManufacturer(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) throws com.fasterxml.jackson.databind.JsonMappingException {
        Manufacturer existing = manufacturerService.getManufacturerById(id);
        new ObjectMapper().updateValue(existing, updates);
        return ResponseEntity.ok(manufacturerService.updateManufacturer(id, existing));
    }

    // Handles HTTP GET requests to retrieve a single resource by ID
    @GetMapping("/{id}")
    public ResponseEntity<Manufacturer> getManufacturerById(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        Manufacturer manufacturer = manufacturerService.getManufacturerById(id);
        return ResponseEntity.ok(manufacturer);
    }

    // Handles HTTP GET requests to retrieve a single resource by ID
    @GetMapping("/name/{name}")
    public ResponseEntity<Manufacturer> getManufacturerByName(
            // Extracts the {id} value from the URL path
            @PathVariable String name) {
        Manufacturer manufacturer = manufacturerService.getManufacturerByName(name);
        return ResponseEntity.ok(manufacturer);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping
    public ResponseEntity<List<Manufacturer>> getAllManufacturers() {
        List<Manufacturer> manufacturers = manufacturerService.getAllManufacturers();
        return ResponseEntity.ok(manufacturers);
    }

    // Handles HTTP DELETE requests to remove a resource by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteManufacturer(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        manufacturerService.deleteManufacturer(id);
        return ResponseEntity.noContent().build();
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/check/name/{name}")
    public ResponseEntity<Boolean> checkName(
            // Extracts the {id} value from the URL path
            @PathVariable String name) {
        return ResponseEntity.ok(manufacturerService.existsByName(name));
    }
}
