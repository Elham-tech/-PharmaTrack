/*
 * ARCHITECTURE: Service layer sits between controller and DAO. It owns all business rules
 * (duplicate checks, business validations) and transaction boundaries. @Transactional at
 * class level means every method runs inside a database transaction by default - if an
 * exception is thrown, all changes are rolled back.
 */
package com.example.PharmaTrack.service.impl;

import com.example.PharmaTrack.entity.AuditLog.AuditAction;
import com.example.PharmaTrack.entity.InventoryBatch;
import com.example.PharmaTrack.dao.InventoryBatchDAO;
import com.example.PharmaTrack.security.CurrentUserProvider;
import com.example.PharmaTrack.service.AuditLogService;
import com.example.PharmaTrack.service.InventoryBatchService;
import com.example.PharmaTrack.exception.ResourceNotFoundException;
import com.example.PharmaTrack.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// Marks this class as a Spring-managed service (business logic layer)
@Service
// Makes all methods transactional by default (auto-commits or rolls back on exception)
@Transactional
public class InventoryBatchServiceImpl implements InventoryBatchService {

    private final InventoryBatchDAO inventoryBatchDAO;
    private final AuditLogService auditLogService;
    private final CurrentUserProvider currentUserProvider;

    @Autowired
    public InventoryBatchServiceImpl(InventoryBatchDAO inventoryBatchDAO,
                                     AuditLogService auditLogService,
                                     CurrentUserProvider currentUserProvider) {
        this.inventoryBatchDAO = inventoryBatchDAO;
        this.auditLogService = auditLogService;
        this.currentUserProvider = currentUserProvider;
    }

    // Indicates this method implements an interface contract
    @Override
    public InventoryBatch createInventoryBatch(InventoryBatch inventoryBatch) {
        if (inventoryBatchDAO.findByBatchNumber(inventoryBatch.getBatchNumber()) != null) {
            throw new BadRequestException("Batch number already exists: " + inventoryBatch.getBatchNumber());
        }
        // ARCHITECTURE: Quantity is auto-managed by stock movements (stock-in/stock-out),
        // so it always starts at 0 and is never accepted from the request body.
        inventoryBatch.setQuantity(0);
        inventoryBatch.setQuantityRemaining(0);
        InventoryBatch created = inventoryBatchDAO.save(inventoryBatch);
        auditLogService.logAction("InventoryBatch", created.getId(), AuditAction.CREATE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            null, "batchNumber=" + created.getBatchNumber());
        return created;
    }

    // Indicates this method implements an interface contract
    @Override
    public InventoryBatch updateInventoryBatch(Long id, InventoryBatch inventoryBatch) {
        InventoryBatch existingBatch = inventoryBatchDAO.findById(id);
        if (existingBatch == null) {
            throw new ResourceNotFoundException("InventoryBatch", "id", id);
        }
        String oldValues = "batchNumber=" + existingBatch.getBatchNumber();
        existingBatch.setBatchNumber(inventoryBatch.getBatchNumber());
        existingBatch.setMedicine(inventoryBatch.getMedicine());
        existingBatch.setSupplier(inventoryBatch.getSupplier());
        // Quantity is auto-managed by stock movements, never edited directly.
        existingBatch.setUnitCost(inventoryBatch.getUnitCost());
        existingBatch.setUnitPrice(inventoryBatch.getUnitPrice());
        existingBatch.setManufacturingDate(inventoryBatch.getManufacturingDate());
        existingBatch.setExpiryDate(inventoryBatch.getExpiryDate());
        InventoryBatch saved = inventoryBatchDAO.save(existingBatch);
        auditLogService.logAction("InventoryBatch", id, AuditAction.UPDATE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            oldValues, "batchNumber=" + saved.getBatchNumber());
        return saved;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public InventoryBatch getInventoryBatchById(Long id) {
        InventoryBatch inventoryBatch = inventoryBatchDAO.findById(id);
        if (inventoryBatch == null) {
            throw new ResourceNotFoundException("InventoryBatch", "id", id);
        }
        return inventoryBatch;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public InventoryBatch getInventoryBatchByNumber(String batchNumber) {
        InventoryBatch inventoryBatch = inventoryBatchDAO.findByBatchNumber(batchNumber);
        if (inventoryBatch == null) {
            throw new ResourceNotFoundException("InventoryBatch", "batchNumber", batchNumber);
        }
        return inventoryBatch;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<InventoryBatch> getAllInventoryBatches() {
        return inventoryBatchDAO.findAll();
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<InventoryBatch> getBatchesByMedicine(Long medicineId) {
        return inventoryBatchDAO.findByMedicineId(medicineId);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<InventoryBatch> getBatchesBySupplier(Long supplierId) {
        return inventoryBatchDAO.findBySupplierId(supplierId);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<InventoryBatch> getExpiringBatches(LocalDate expiryDate) {
        return inventoryBatchDAO.findExpiringBatches(expiryDate);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<InventoryBatch> getAvailableBatchesByMedicine(Long medicineId) {
        return inventoryBatchDAO.findAvailableBatchesByMedicineId(medicineId);
    }

    // Indicates this method implements an interface contract
    @Override
    public void deleteInventoryBatch(Long id) {
        if (!inventoryBatchDAO.existsById(id)) {
            throw new ResourceNotFoundException("InventoryBatch", "id", id);
        }
        inventoryBatchDAO.deleteById(id);
        auditLogService.logAction("InventoryBatch", id, AuditAction.DELETE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            "id=" + id, null);
    }
}
