/*
 * ARCHITECTURE: Service layer sits between controller and DAO. It owns all business rules
 * (duplicate checks, business validations) and transaction boundaries. @Transactional at
 * class level means every method runs inside a database transaction by default - if an
 * exception is thrown, all changes are rolled back.
 */
package com.example.PharmaTrack.service.impl;

import com.example.PharmaTrack.entity.AuditLog.AuditAction;
import com.example.PharmaTrack.entity.Manufacturer;
import com.example.PharmaTrack.dao.ManufacturerDAO;
import com.example.PharmaTrack.security.CurrentUserProvider;
import com.example.PharmaTrack.service.AuditLogService;
import com.example.PharmaTrack.service.ManufacturerService;
import com.example.PharmaTrack.exception.ResourceNotFoundException;
import com.example.PharmaTrack.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Marks this class as a Spring-managed service (business logic layer)
@Service
// Makes all methods transactional by default (auto-commits or rolls back on exception)
@Transactional
public class ManufacturerServiceImpl implements ManufacturerService {

    private final ManufacturerDAO manufacturerDAO;
    private final AuditLogService auditLogService;
    private final CurrentUserProvider currentUserProvider;

    @Autowired
    public ManufacturerServiceImpl(ManufacturerDAO manufacturerDAO,
                                   AuditLogService auditLogService,
                                   CurrentUserProvider currentUserProvider) {
        this.manufacturerDAO = manufacturerDAO;
        this.auditLogService = auditLogService;
        this.currentUserProvider = currentUserProvider;
    }

    // Indicates this method implements an interface contract
    @Override
    public Manufacturer createManufacturer(Manufacturer manufacturer) {
        if (manufacturerDAO.existsByName(manufacturer.getName())) {
            throw new BadRequestException("Manufacturer name already exists: " + manufacturer.getName());
        }
        Manufacturer created = manufacturerDAO.save(manufacturer);
        auditLogService.logAction("Manufacturer", created.getId(), AuditAction.CREATE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            null, "name=" + created.getName());
        return created;
    }

    // Indicates this method implements an interface contract
    @Override
    public Manufacturer updateManufacturer(Long id, Manufacturer manufacturer) {
        Manufacturer existingManufacturer = manufacturerDAO.findById(id);
        if (existingManufacturer == null) {
            throw new ResourceNotFoundException("Manufacturer", "id", id);
        }
        String oldValues = "name=" + existingManufacturer.getName();
        existingManufacturer.setName(manufacturer.getName());
        existingManufacturer.setAddress(manufacturer.getAddress());
        existingManufacturer.setPhone(manufacturer.getPhone());
        existingManufacturer.setEmail(manufacturer.getEmail());
        existingManufacturer.setCountry(manufacturer.getCountry());
        Manufacturer saved = manufacturerDAO.save(existingManufacturer);
        auditLogService.logAction("Manufacturer", id, AuditAction.UPDATE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            oldValues, "name=" + saved.getName());
        return saved;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public Manufacturer getManufacturerById(Long id) {
        Manufacturer manufacturer = manufacturerDAO.findById(id);
        if (manufacturer == null) {
            throw new ResourceNotFoundException("Manufacturer", "id", id);
        }
        return manufacturer;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public Manufacturer getManufacturerByName(String name) {
        Manufacturer manufacturer = manufacturerDAO.findByName(name);
        if (manufacturer == null) {
            throw new ResourceNotFoundException("Manufacturer", "name", name);
        }
        return manufacturer;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<Manufacturer> getAllManufacturers() {
        return manufacturerDAO.findAll();
    }

    // Indicates this method implements an interface contract
    @Override
    public void deleteManufacturer(Long id) {
        if (!manufacturerDAO.existsById(id)) {
            throw new ResourceNotFoundException("Manufacturer", "id", id);
        }
        manufacturerDAO.deleteById(id);
        auditLogService.logAction("Manufacturer", id, AuditAction.DELETE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            "id=" + id, null);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return manufacturerDAO.existsByName(name);
    }
}
