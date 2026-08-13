/*
 * ARCHITECTURE: Service layer sits between controller and DAO. It owns all business rules
 * (duplicate checks, business validations) and transaction boundaries. @Transactional at
 * class level means every method runs inside a database transaction by default - if an
 * exception is thrown, all changes are rolled back.
 */
package com.example.PharmaTrack.service.impl;

import com.example.PharmaTrack.entity.AuditLog.AuditAction;
import com.example.PharmaTrack.entity.Supplier;
import com.example.PharmaTrack.dao.SupplierDAO;
import com.example.PharmaTrack.security.CurrentUserProvider;
import com.example.PharmaTrack.service.AuditLogService;
import com.example.PharmaTrack.service.SupplierService;
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
public class SupplierServiceImpl implements SupplierService {

    private final SupplierDAO supplierDAO;
    private final AuditLogService auditLogService;
    private final CurrentUserProvider currentUserProvider;

    @Autowired
    public SupplierServiceImpl(SupplierDAO supplierDAO,
                               AuditLogService auditLogService,
                               CurrentUserProvider currentUserProvider) {
        this.supplierDAO = supplierDAO;
        this.auditLogService = auditLogService;
        this.currentUserProvider = currentUserProvider;
    }

    // Indicates this method implements an interface contract
    @Override
    public Supplier createSupplier(Supplier supplier) {
        if (supplierDAO.findByCode(supplier.getCode()) != null) {
            throw new BadRequestException("Supplier code already exists: " + supplier.getCode());
        }
        Supplier created = supplierDAO.save(supplier);
        auditLogService.logAction("Supplier", created.getId(), AuditAction.CREATE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            null, "code=" + created.getCode() + ", name=" + created.getName());
        return created;
    }

    // Indicates this method implements an interface contract
    @Override
    public Supplier updateSupplier(Long id, Supplier supplier) {
        Supplier existingSupplier = supplierDAO.findById(id);
        if (existingSupplier == null) {
            throw new ResourceNotFoundException("Supplier", "id", id);
        }
        String oldValues = "code=" + existingSupplier.getCode() + ", name=" + existingSupplier.getName();
        existingSupplier.setCode(supplier.getCode());
        existingSupplier.setName(supplier.getName());
        existingSupplier.setContactPerson(supplier.getContactPerson());
        existingSupplier.setPhone(supplier.getPhone());
        existingSupplier.setEmail(supplier.getEmail());
        existingSupplier.setAddress(supplier.getAddress());
        existingSupplier.setCity(supplier.getCity());
        existingSupplier.setCountry(supplier.getCountry());
        Supplier saved = supplierDAO.save(existingSupplier);
        auditLogService.logAction("Supplier", id, AuditAction.UPDATE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            oldValues, "code=" + saved.getCode() + ", name=" + saved.getName());
        return saved;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public Supplier getSupplierById(Long id) {
        Supplier supplier = supplierDAO.findById(id);
        if (supplier == null) {
            throw new ResourceNotFoundException("Supplier", "id", id);
        }
        return supplier;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public Supplier getSupplierByCode(String code) {
        Supplier supplier = supplierDAO.findByCode(code);
        if (supplier == null) {
            throw new ResourceNotFoundException("Supplier", "code", code);
        }
        return supplier;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers() {
        return supplierDAO.findAll();
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<Supplier> getActiveSuppliers() {
        return supplierDAO.findByActive(true);
    }

    // Indicates this method implements an interface contract
    @Override
    public void deleteSupplier(Long id) {
        if (!supplierDAO.existsById(id)) {
            throw new ResourceNotFoundException("Supplier", "id", id);
        }
        supplierDAO.deleteById(id);
        auditLogService.logAction("Supplier", id, AuditAction.DELETE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            "id=" + id, null);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        return supplierDAO.findByCode(code) != null;
    }
}
