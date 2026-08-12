/*
 * ARCHITECTURE: Service layer sits between controller and DAO. It owns all business rules
 * (duplicate checks, business validations) and transaction boundaries. @Transactional at
 * class level means every method runs inside a database transaction by default - if an
 * exception is thrown, all changes are rolled back.
 */
package com.example.PharmaTrack.service.impl;

import com.example.PharmaTrack.entity.AuditLog.AuditAction;
import com.example.PharmaTrack.entity.DispensingRecord;
import com.example.PharmaTrack.entity.InventoryBatch;
import com.example.PharmaTrack.entity.Prescription;
import com.example.PharmaTrack.entity.StockMovement;
import com.example.PharmaTrack.entity.StockMovement.MovementType;
import com.example.PharmaTrack.entity.User;
import com.example.PharmaTrack.dao.DispensingRecordDAO;
import com.example.PharmaTrack.dao.InventoryBatchDAO;
import com.example.PharmaTrack.dao.PrescriptionDAO;
import com.example.PharmaTrack.dao.StockMovementDAO;
import com.example.PharmaTrack.dao.UserDAO;
import com.example.PharmaTrack.security.CurrentUserProvider;
import com.example.PharmaTrack.service.AuditLogService;
import com.example.PharmaTrack.service.DispensingRecordService;
import com.example.PharmaTrack.exception.ResourceNotFoundException;
import com.example.PharmaTrack.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

// Marks this class as a Spring-managed service (business logic layer)
@Service
// Makes all methods transactional by default (auto-commits or rolls back on exception)
@Transactional
public class DispensingRecordServiceImpl implements DispensingRecordService {

    private final DispensingRecordDAO dispensingRecordDAO;
    private final InventoryBatchDAO inventoryBatchDAO;
    private final StockMovementDAO stockMovementDAO;
    private final UserDAO userDAO;
    private final PrescriptionDAO prescriptionDAO;
    private final AuditLogService auditLogService;
    private final CurrentUserProvider currentUserProvider;

    @Autowired
    public DispensingRecordServiceImpl(DispensingRecordDAO dispensingRecordDAO,
                                        InventoryBatchDAO inventoryBatchDAO,
                                        StockMovementDAO stockMovementDAO,
                                        UserDAO userDAO,
                                        PrescriptionDAO prescriptionDAO,
                                        AuditLogService auditLogService,
                                        CurrentUserProvider currentUserProvider) {
        this.dispensingRecordDAO = dispensingRecordDAO;
        this.inventoryBatchDAO = inventoryBatchDAO;
        this.stockMovementDAO = stockMovementDAO;
        this.userDAO = userDAO;
        this.prescriptionDAO = prescriptionDAO;
        this.auditLogService = auditLogService;
        this.currentUserProvider = currentUserProvider;
    }

    /*
     * ARCHITECTURE: This method performs 3 related writes (deduct batch, log stock movement, save record)
     * in a single @Transactional method so that if ANY step fails (e.g. insufficient stock,
     * database error), the entire operation rolls back. This prevents partial updates like
     * deducting inventory without recording the dispensing, which would cause data inconsistency.
     */
    @Override
    public DispensingRecord createDispensingRecord(DispensingRecord dispensingRecord) {
        if (dispensingRecordDAO.findByDispensingNumber(dispensingRecord.getDispensingNumber()) != null) {
            throw new BadRequestException("Dispensing number already exists: " + dispensingRecord.getDispensingNumber());
        }

        InventoryBatch batch = inventoryBatchDAO.findById(dispensingRecord.getInventoryBatch().getId());
        if (batch == null) {
            throw new ResourceNotFoundException("InventoryBatch", "id", dispensingRecord.getInventoryBatch().getId());
        }
        if (batch.getQuantityRemaining() < dispensingRecord.getQuantityDispensed()) {
            throw new BadRequestException("Insufficient stock. Available: " + batch.getQuantityRemaining() + ", Requested: " + dispensingRecord.getQuantityDispensed());
        }

        /*
         * ARCHITECTURE: Dispensing is priced at the batch unit cost plus a 20% markup so the
         * pharmacy makes a profit. The price is computed server-side from the selected batch's
         * cost (never trusted from the client), so the benefit is always applied. Stock-out
         * movements are priced at the same (cost) price - no markup is applied there.
         */
        BigDecimal unitPrice = batch.getUnitCost()
                .multiply(BigDecimal.valueOf(1.2))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPrice = unitPrice
                .multiply(BigDecimal.valueOf(dispensingRecord.getQuantityDispensed()))
                .setScale(2, RoundingMode.HALF_UP);
        dispensingRecord.setUnitPrice(unitPrice);
        dispensingRecord.setTotalPrice(totalPrice);

        // ARCHITECTURE: quantity tracks total ever received (only grows on stock-in),
        // while quantityRemaining is the live balance, so dispensing only drops the latter.
        batch.setQuantityRemaining(batch.getQuantityRemaining() - dispensingRecord.getQuantityDispensed());
        inventoryBatchDAO.save(batch);

        StockMovement movement = new StockMovement(
            MovementType.STOCK_OUT,
            dispensingRecord.getMedicine(),
            batch,
            dispensingRecord.getQuantityDispensed(),
            dispensingRecord.getDispensedBy()
        );
        movement.setReferenceNumber(dispensingRecord.getDispensingNumber());
        movement.setNotes("Dispensed via prescription " + dispensingRecord.getPrescription().getPrescriptionNumber());
        stockMovementDAO.save(movement);

        DispensingRecord saved = dispensingRecordDAO.save(dispensingRecord);
        auditLogService.logAction("DispensingRecord", saved.getId(), AuditAction.DISPENSE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            null, "dispensingNumber=" + saved.getDispensingNumber()
                + ", quantity=" + saved.getQuantityDispensed()
                + ", total=" + saved.getTotalPrice());
        return saved;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public DispensingRecord getDispensingRecordById(Long id) {
        DispensingRecord dispensingRecord = dispensingRecordDAO.findById(id);
        if (dispensingRecord == null) {
            throw new ResourceNotFoundException("DispensingRecord", "id", id);
        }
        return dispensingRecord;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public DispensingRecord getDispensingRecordByNumber(String dispensingNumber) {
        DispensingRecord dispensingRecord = dispensingRecordDAO.findByDispensingNumber(dispensingNumber);
        if (dispensingRecord == null) {
            throw new ResourceNotFoundException("DispensingRecord", "dispensingNumber", dispensingNumber);
        }
        return dispensingRecord;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<DispensingRecord> getAllDispensingRecords() {
        return dispensingRecordDAO.findAll();
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<DispensingRecord> getDispensingRecordsByPrescription(Long prescriptionId) {
        return dispensingRecordDAO.findByPrescriptionId(prescriptionId);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<DispensingRecord> getDispensingRecordsByMedicine(Long medicineId) {
        return dispensingRecordDAO.findByMedicineId(medicineId);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<DispensingRecord> getDispensingRecordsByUser(Long userId) {
        return dispensingRecordDAO.findByDispensedById(userId);
    }

    /*
     * ARCHITECTURE: Cashier workflow. A record is created as PENDING, then the cashier either
     * approves it (payment processed -> PAID) or voids it (not paid -> VOIDED). Both methods
     * run in a transaction so the status change and any inventory/prescription side effects
     * are atomic.
     */
    @Override
    public DispensingRecord approvePayment(Long id) {
        DispensingRecord record = dispensingRecordDAO.findById(id);
        if (record == null) {
            throw new ResourceNotFoundException("DispensingRecord", "id", id);
        }
        if (record.getPaymentStatus() != DispensingRecord.PaymentStatus.PENDING) {
            throw new BadRequestException("Only pending dispensing records can be approved");
        }
        // The cashier performing the approval is the currently logged-in user.
        Long cashierId = currentUserProvider.getCurrentUserId();
        User cashier = cashierId == null ? null : userDAO.findById(cashierId);
        if (cashier == null) {
            throw new BadRequestException("Unable to determine the logged-in user performing the approval");
        }
        record.setPaymentStatus(DispensingRecord.PaymentStatus.PAID);
        record.setProcessedBy(cashier);
        record.setProcessedAt(LocalDateTime.now());
        DispensingRecord saved = dispensingRecordDAO.save(record);
        auditLogService.logAction("DispensingRecord", id, AuditAction.UPDATE,
            cashier.getId(), currentUserProvider.getClientIp(),
            "paymentStatus=PENDING", "dispensingNumber=" + saved.getDispensingNumber() + ", paymentStatus=" + saved.getPaymentStatus());
        return saved;
    }

    @Override
    public DispensingRecord voidDispensing(Long id) {
        DispensingRecord record = dispensingRecordDAO.findById(id);
        if (record == null) {
            throw new ResourceNotFoundException("DispensingRecord", "id", id);
        }
        if (record.getPaymentStatus() != DispensingRecord.PaymentStatus.PENDING) {
            throw new BadRequestException("Only pending dispensing records can be voided");
        }
        // The cashier performing the void is the currently logged-in user.
        Long cashierId = currentUserProvider.getCurrentUserId();
        User cashier = cashierId == null ? null : userDAO.findById(cashierId);
        if (cashier == null) {
            throw new BadRequestException("Unable to determine the logged-in user performing the void");
        }

        // Return the medicine to the batch stock and log a STOCK_IN movement.
        InventoryBatch batch = record.getInventoryBatch();
        batch.setQuantityRemaining(batch.getQuantityRemaining() + record.getQuantityDispensed());
        inventoryBatchDAO.save(batch);

        StockMovement movement = new StockMovement(
            MovementType.STOCK_IN,
            record.getMedicine(),
            batch,
            record.getQuantityDispensed(),
            cashier
        );
        movement.setReferenceNumber(record.getDispensingNumber());
        movement.setNotes("Voided dispensing " + record.getDispensingNumber());
        stockMovementDAO.save(movement);

        // Mark the linked prescription as voided so it shows Voided and cannot be dispensed.
        Prescription prescription = record.getPrescription();
        if (prescription != null) {
            prescription.setVoided(true);
            prescription.setDispensed(false);
            prescription.setDispensedDate(null);
            prescription.setDispensedBy(null);
            prescriptionDAO.save(prescription);
        }

        record.setPaymentStatus(DispensingRecord.PaymentStatus.VOIDED);
        record.setProcessedBy(cashier);
        record.setProcessedAt(LocalDateTime.now());
        DispensingRecord saved = dispensingRecordDAO.save(record);
        auditLogService.logAction("DispensingRecord", id, AuditAction.UPDATE,
            cashier.getId(), currentUserProvider.getClientIp(),
            "paymentStatus=PENDING", "dispensingNumber=" + saved.getDispensingNumber() + ", paymentStatus=" + saved.getPaymentStatus());
        return saved;
    }
}
