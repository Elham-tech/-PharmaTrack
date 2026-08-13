/*
 * ARCHITECTURE: Service layer sits between controller and DAO. It owns all business rules
 * (duplicate checks, business validations) and transaction boundaries. @Transactional at
 * class level means every method runs inside a database transaction by default - if an
 * exception is thrown, all changes are rolled back. @Transactional(readOnly = true) on read
 * methods tells Hibernate to skip dirty checking and use a read-only connection, improving
 * performance for queries that don't modify data.
 */
package com.example.PharmaTrack.service.impl;

import com.example.PharmaTrack.dto.StockMovementRequest;
import com.example.PharmaTrack.entity.AuditLog.AuditAction;
import com.example.PharmaTrack.entity.InventoryBatch;
import com.example.PharmaTrack.entity.Medicine;
import com.example.PharmaTrack.entity.StockMovement;
import com.example.PharmaTrack.entity.StockMovement.MovementType;
import com.example.PharmaTrack.entity.User;
import com.example.PharmaTrack.dao.InventoryBatchDAO;
import com.example.PharmaTrack.dao.MedicineDAO;
import com.example.PharmaTrack.dao.StockMovementDAO;
import com.example.PharmaTrack.dao.UserDAO;
import com.example.PharmaTrack.security.CurrentUserProvider;
import com.example.PharmaTrack.service.AuditLogService;
import com.example.PharmaTrack.service.StockMovementService;
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
public class StockMovementServiceImpl implements StockMovementService {

    private final StockMovementDAO stockMovementDAO;
    private final InventoryBatchDAO inventoryBatchDAO;
    private final MedicineDAO medicineDAO;
    private final UserDAO userDAO;
    private final AuditLogService auditLogService;
    private final CurrentUserProvider currentUserProvider;

    @Autowired
    public StockMovementServiceImpl(StockMovementDAO stockMovementDAO,
                                     InventoryBatchDAO inventoryBatchDAO,
                                     MedicineDAO medicineDAO,
                                     UserDAO userDAO,
                                     AuditLogService auditLogService,
                                     CurrentUserProvider currentUserProvider) {
        this.stockMovementDAO = stockMovementDAO;
        this.inventoryBatchDAO = inventoryBatchDAO;
        this.medicineDAO = medicineDAO;
        this.userDAO = userDAO;
        this.auditLogService = auditLogService;
        this.currentUserProvider = currentUserProvider;
    }

    // Indicates this method implements an interface contract
    @Override
    public StockMovement createStockMovement(StockMovement stockMovement) {
        StockMovement created = stockMovementDAO.save(stockMovement);
        auditLogService.logAction("StockMovement", created.getId(), AuditAction.CREATE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            null, "movementType=" + created.getMovementType());
        return created;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public StockMovement getStockMovementById(Long id) {
        StockMovement stockMovement = stockMovementDAO.findById(id);
        if (stockMovement == null) {
            throw new ResourceNotFoundException("StockMovement", "id", id);
        }
        return stockMovement;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<StockMovement> getAllStockMovements() {
        return stockMovementDAO.findAll();
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<StockMovement> getStockMovementsByType(MovementType movementType) {
        return stockMovementDAO.findByMovementType(movementType);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<StockMovement> getStockMovementsByMedicine(Long medicineId) {
        return stockMovementDAO.findByMedicineId(medicineId);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<StockMovement> getStockMovementsByBatch(Long batchId) {
        return stockMovementDAO.findByInventoryBatchId(batchId);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<StockMovement> getStockMovementsByUser(Long userId) {
        return stockMovementDAO.findByPerformedById(userId);
    }

    /*
     * ARCHITECTURE: Stock-in and stock-out both update batch quantity + log a movement record
     * in a single transaction. If the batch lookup fails or the save throws an exception,
     * neither the quantity nor the movement record is persisted, keeping inventory accurate.
     */
    @Override
    public void processStockIn(StockMovementRequest request) {
        InventoryBatch batch = inventoryBatchDAO.findById(request.getInventoryBatchId());
        if (batch == null) {
            throw new ResourceNotFoundException("InventoryBatch", "id", request.getInventoryBatchId());
        }
        Medicine medicine = medicineDAO.findById(request.getMedicineId());
        if (medicine == null) {
            throw new ResourceNotFoundException("Medicine", "id", request.getMedicineId());
        }
        // The user performing the movement is always the currently logged-in user.
        Long userId = currentUserProvider.getCurrentUserId();
        User user = userId == null ? null : userDAO.findById(userId);
        if (user == null) {
            throw new BadRequestException("Unable to determine the logged-in user performing the stock-in");
        }
        batch.setQuantity(batch.getQuantity() + request.getQuantity());
        batch.setQuantityRemaining(batch.getQuantityRemaining() + request.getQuantity());
        inventoryBatchDAO.save(batch);
        StockMovement movement = new StockMovement(MovementType.STOCK_IN, medicine, batch, request.getQuantity(), user);
        movement.setReferenceNumber(request.getReferenceNumber());
        movement.setNotes(request.getNotes());
        stockMovementDAO.save(movement);
        auditLogService.logAction("StockMovement", movement.getId(), AuditAction.STOCK_IN,
            user.getId(), currentUserProvider.getClientIp(),
            null, "batch=" + batch.getBatchNumber() + ", quantity=" + movement.getQuantity());
    }

    /*
     * ARCHITECTURE: Validates sufficient stock before deducting. The null check prevents
     * NullPointerException if the batch was deleted. The quantity check prevents negative
     * inventory. Both validations happen BEFORE any write, so no partial state is left.
     */
    @Override
    public void processStockOut(StockMovementRequest request) {
        InventoryBatch batch = inventoryBatchDAO.findById(request.getInventoryBatchId());
        if (batch == null) {
            throw new ResourceNotFoundException("InventoryBatch", "id", request.getInventoryBatchId());
        }
        if (batch.getQuantityRemaining() < request.getQuantity()) {
            throw new BadRequestException("Insufficient stock. Available: " + batch.getQuantityRemaining() + ", Requested: " + request.getQuantity());
        }
        Medicine medicine = medicineDAO.findById(request.getMedicineId());
        if (medicine == null) {
            throw new ResourceNotFoundException("Medicine", "id", request.getMedicineId());
        }
        // The user performing the movement is always the currently logged-in user.
        Long userId = currentUserProvider.getCurrentUserId();
        User user = userId == null ? null : userDAO.findById(userId);
        if (user == null) {
            throw new BadRequestException("Unable to determine the logged-in user performing the stock-out");
        }
        // ARCHITECTURE: quantity tracks total ever received (only grows on stock-in),
        // while quantityRemaining is the live balance, so stock-out only drops the latter.
        batch.setQuantityRemaining(batch.getQuantityRemaining() - request.getQuantity());
        inventoryBatchDAO.save(batch);
        StockMovement movement = new StockMovement(MovementType.STOCK_OUT, medicine, batch, request.getQuantity(), user);
        movement.setReferenceNumber(request.getReferenceNumber());
        movement.setNotes(request.getNotes());
        stockMovementDAO.save(movement);
        auditLogService.logAction("StockMovement", movement.getId(), AuditAction.STOCK_OUT,
            user.getId(), currentUserProvider.getClientIp(),
            null, "batch=" + batch.getBatchNumber() + ", quantity=" + movement.getQuantity());
    }
}
