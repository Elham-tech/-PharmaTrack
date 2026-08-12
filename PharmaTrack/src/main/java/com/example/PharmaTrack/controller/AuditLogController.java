package com.example.PharmaTrack.controller;

import com.example.PharmaTrack.entity.AuditLog;
import com.example.PharmaTrack.entity.AuditLog.AuditAction;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.PharmaTrack.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;

// Marks this class as a REST controller; all methods return JSON responses
@RestController
// Base URL path for all endpoints in this controller
@RequestMapping("/api/audit-logs")
// Allows requests from any origin (CORS) - restrict in production
@CrossOrigin(origins = "*")
// Role-based access: ADMIN and AUDITOR only
@PreAuthorize("hasAnyAuthority(@appAuthorities.admin, @appAuthorities.auditor)")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Autowired
    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    // Handles HTTP POST requests to create a new resource
    @PostMapping
    // Deserializes the HTTP request body JSON into an AuditLog object
    public ResponseEntity<AuditLog> createAuditLog(@Valid @RequestBody AuditLog auditLog) {
        AuditLog createdLog = auditLogService.createAuditLog(auditLog);
        return new ResponseEntity<>(createdLog, HttpStatus.CREATED);
    }

    // Handles HTTP GET requests to retrieve a single resource by ID
    @GetMapping("/{id}")
    public ResponseEntity<AuditLog> getAuditLogById(
            // Extracts the {id} value from the URL path
            @PathVariable Long id) {
        AuditLog log = auditLogService.getAuditLogById(id);
        return ResponseEntity.ok(log);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllAuditLogs() {
        List<AuditLog> logs = auditLogService.getAllAuditLogs();
        return ResponseEntity.ok(logs);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/entity-type/{entityType}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByEntityType(
            // Extracts the {id} value from the URL path
            @PathVariable String entityType) {
        List<AuditLog> logs = auditLogService.getAuditLogsByEntityType(entityType);
        return ResponseEntity.ok(logs);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByEntity(
            // Extracts the {id} value from the URL path
            @PathVariable String entityType,
            // Extracts the {id} value from the URL path
            @PathVariable Long entityId) {
        List<AuditLog> logs = auditLogService.getAuditLogsByEntity(entityType, entityId);
        return ResponseEntity.ok(logs);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/action/{action}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByAction(
            // Extracts the {id} value from the URL path
            @PathVariable AuditAction action) {
        List<AuditLog> logs = auditLogService.getAuditLogsByAction(action);
        return ResponseEntity.ok(logs);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUser(
            // Extracts the {id} value from the URL path
            @PathVariable Long userId) {
        List<AuditLog> logs = auditLogService.getAuditLogsByUser(userId);
        return ResponseEntity.ok(logs);
    }

    // Handles HTTP GET requests to retrieve all resources
    @GetMapping("/date-range")
    public ResponseEntity<List<AuditLog>> getAuditLogsByDateRange(
            // Extracts the value from the query parameter ?name=...
            @RequestParam
            // Tells Spring to parse the query param as an ISO-8601 datetime string
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            // Extracts the value from the query parameter ?name=...
            @RequestParam
            // Tells Spring to parse the query param as an ISO-8601 datetime string
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<AuditLog> logs = auditLogService.getAuditLogsByDateRange(start, end);
        return ResponseEntity.ok(logs);
    }

    // Handles HTTP POST requests to create a new resource
    @PostMapping("/log")
    public ResponseEntity<Void> logAction(
            // Extracts the value from the query parameter ?name=...
            @RequestParam String entityType,
            // Extracts the value from the query parameter ?name=...
            @RequestParam Long entityId,
            // Extracts the value from the query parameter ?name=...
            @RequestParam AuditAction action,
            // Extracts the value from the query parameter ?name=...
            @RequestParam Long userId,
            // Extracts the value from the query parameter ?name=...
            @RequestParam String ipAddress,
            // Optional query parameter that can be omitted
            @RequestParam(required = false) String oldValues,
            // Optional query parameter that can be omitted
            @RequestParam(required = false) String newValues) {
        auditLogService.logAction(entityType, entityId, action, userId, ipAddress, oldValues, newValues);
        return ResponseEntity.ok().build();
    }
}
