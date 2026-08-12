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
import com.example.PharmaTrack.entity.PrescriptionItem;
import com.example.PharmaTrack.entity.User;
import com.example.PharmaTrack.entity.Medicine;
import com.example.PharmaTrack.dao.PrescriptionDAO;
import com.example.PharmaTrack.dao.UserDAO;
import com.example.PharmaTrack.dao.MedicineDAO;
import com.example.PharmaTrack.dao.InventoryBatchDAO;
import com.example.PharmaTrack.security.CurrentUserProvider;
import com.example.PharmaTrack.service.AuditLogService;
import com.example.PharmaTrack.service.DispensingRecordService;
import com.example.PharmaTrack.service.PrescriptionService;
import com.example.PharmaTrack.exception.ResourceNotFoundException;
import com.example.PharmaTrack.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Marks this class as a Spring-managed service (business logic layer)
@Service
// Makes all methods transactional by default (auto-commits or rolls back on exception)
@Transactional
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionDAO prescriptionDAO;
    private final UserDAO userDAO;
    private final MedicineDAO medicineDAO;
    private final InventoryBatchDAO inventoryBatchDAO;
    private final DispensingRecordService dispensingRecordService;
    private final AuditLogService auditLogService;
    private final CurrentUserProvider currentUserProvider;

    @Autowired
    public PrescriptionServiceImpl(PrescriptionDAO prescriptionDAO,
                                    UserDAO userDAO,
                                    MedicineDAO medicineDAO,
                                    InventoryBatchDAO inventoryBatchDAO,
                                    DispensingRecordService dispensingRecordService,
                                    AuditLogService auditLogService,
                                    CurrentUserProvider currentUserProvider) {
        this.prescriptionDAO = prescriptionDAO;
        this.userDAO = userDAO;
        this.medicineDAO = medicineDAO;
        this.inventoryBatchDAO = inventoryBatchDAO;
        this.dispensingRecordService = dispensingRecordService;
        this.auditLogService = auditLogService;
        this.currentUserProvider = currentUserProvider;
    }

    // Indicates this method implements an interface contract
    @Override
    public Prescription createPrescription(Prescription prescription) {
        if (prescriptionDAO.findByPrescriptionNumber(prescription.getPrescriptionNumber()) != null) {
            throw new BadRequestException("Prescription number already exists: " + prescription.getPrescriptionNumber());
        }
        if (prescription.getItems() == null || prescription.getItems().isEmpty()) {
            throw new BadRequestException("A prescription must contain at least one medicine item");
        }
        for (PrescriptionItem item : prescription.getItems()) {
            item.setPrescription(prescription);
        }
        Prescription created = prescriptionDAO.save(prescription);
        auditLogService.logAction("Prescription", created.getId(), AuditAction.CREATE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            null, "prescriptionNumber=" + created.getPrescriptionNumber() + ", patient=" + created.getPatientName());

        /*
         * ARCHITECTURE: The pharmacist enters the prescription once and dispensing happens
         * automatically - there is no separate dispense step. Every item is dispensed from
         * available stock (priced at cost + 20% markup server-side), then the prescription
         * is marked as dispensed. The cashier only approves (PAID) or voids (VOIDED) the
         * resulting dispensing records. All of this runs in a single transaction, so if any
         * item has insufficient stock the whole prescription creation is rolled back.
         */
        Long pharmacistId = currentUserProvider.getCurrentUserId();
        User dispensedBy = pharmacistId == null ? null : userDAO.findById(pharmacistId);
        if (dispensedBy == null) {
            throw new BadRequestException("Unable to determine the user performing the dispensing");
        }

        int itemIndex = 0;
        int dispensingCount = 0;
        for (PrescriptionItem item : created.getItems()) {
            if (item.getMedicine() == null || item.getMedicine().getId() == null) {
                throw new BadRequestException("Every medicine item must reference a medicine");
            }
            // Load the full medicine entity - the request body only carries its id.
            Medicine medicine = medicineDAO.findById(item.getMedicine().getId());
            if (medicine == null) {
                throw new ResourceNotFoundException("Medicine", "id", item.getMedicine().getId());
            }
            int requiredQty = item.getTotalQuantity();
            if (requiredQty <= 0) {
                throw new BadRequestException("Each medicine item must have a positive dispensed quantity");
            }
            itemIndex++;
            int subIndex = 0;
            List<InventoryBatch> batches = inventoryBatchDAO.findAvailableBatchesByMedicineId(medicine.getId());
            for (InventoryBatch batch : batches) {
                if (requiredQty <= 0) break;
                int take = Math.min(batch.getQuantityRemaining(), requiredQty);
                if (take <= 0) continue;

                DispensingRecord record = new DispensingRecord(
                    "DSP-" + created.getPrescriptionNumber() + "-" + itemIndex + "-" + (++subIndex),
                    created, medicine, batch, take,
                    null, null, dispensedBy);
                dispensingRecordService.createDispensingRecord(record);
                dispensingCount++;
                requiredQty -= take;
            }
            if (requiredQty > 0) {
                int available = batches.stream().mapToInt(InventoryBatch::getQuantityRemaining).sum();
                throw new BadRequestException("Insufficient stock for " + medicine.getName()
                    + ": required " + item.getTotalQuantity() + ", available " + available);
            }
        }

        // Mark the prescription as dispensed once all items have been dispensed.
        created.setDispensed(true);
        created.setDispensedDate(LocalDateTime.now());
        created.setDispensedBy(dispensedBy);
        Prescription saved = prescriptionDAO.save(created);
        auditLogService.logAction("Prescription", saved.getId(), AuditAction.DISPENSE,
            dispensedBy.getId(), currentUserProvider.getClientIp(),
            null, "prescriptionNumber=" + saved.getPrescriptionNumber()
                + ", auto-dispensed " + dispensingCount + " record(s) across " + itemIndex + " item(s)");
        return saved;
    }

    // Indicates this method implements an interface contract
    @Override
    public Prescription updatePrescription(Long id, Prescription prescription) {
        Prescription existingPrescription = prescriptionDAO.findById(id);
        if (existingPrescription == null) {
            throw new ResourceNotFoundException("Prescription", "id", id);
        }
        if (existingPrescription.isDispensed()) {
            throw new BadRequestException("A dispensed prescription cannot be edited");
        }
        if (existingPrescription.isVoided()) {
            throw new BadRequestException("A voided prescription cannot be edited");
        }
        existingPrescription.setPrescriptionNumber(prescription.getPrescriptionNumber());
        existingPrescription.setPatientName(prescription.getPatientName());
        existingPrescription.setPatientIdNumber(prescription.getPatientIdNumber());
        existingPrescription.setDoctorName(prescription.getDoctorName());
        existingPrescription.setHospitalName(prescription.getHospitalName());
        existingPrescription.setPrescriptionDetails(prescription.getPrescriptionDetails());
        existingPrescription.getItems().clear();
        if (prescription.getItems() != null) {
            for (PrescriptionItem item : prescription.getItems()) {
                item.setPrescription(existingPrescription);
                existingPrescription.getItems().add(item);
            }
        }
        Prescription saved = prescriptionDAO.save(existingPrescription);
        auditLogService.logAction("Prescription", id, AuditAction.UPDATE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            null, "prescriptionNumber=" + saved.getPrescriptionNumber() + ", patient=" + saved.getPatientName());
        return saved;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public Prescription getPrescriptionById(Long id) {
        Prescription prescription = prescriptionDAO.findById(id);
        if (prescription == null) {
            throw new ResourceNotFoundException("Prescription", "id", id);
        }
        return prescription;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public Prescription getPrescriptionByNumber(String prescriptionNumber) {
        Prescription prescription = prescriptionDAO.findByPrescriptionNumber(prescriptionNumber);
        if (prescription == null) {
            throw new ResourceNotFoundException("Prescription", "prescriptionNumber", prescriptionNumber);
        }
        return prescription;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<Prescription> getAllPrescriptions() {
        return prescriptionDAO.findAll();
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<Prescription> getPrescriptionsByPatient(String patientName) {
        return prescriptionDAO.findByPatientName(patientName);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<Prescription> getPrescriptionsByDoctor(String doctorName) {
        return prescriptionDAO.findByDoctorName(doctorName);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<Prescription> getUnDispensedPrescriptions() {
        return prescriptionDAO.findByDispensed(false);
    }

    // Indicates this method implements an interface contract
    @Override
    public Prescription markAsDispensed(Long id) {
        Prescription prescription = prescriptionDAO.findById(id);
        if (prescription == null) {
            throw new ResourceNotFoundException("Prescription", "id", id);
        }
        if (prescription.isDispensed()) {
            throw new BadRequestException("Prescription is already dispensed");
        }
        if (prescription.isVoided()) {
            throw new BadRequestException("Prescription has been voided and cannot be dispensed");
        }
        // The pharmacist performing the dispense is the currently logged-in user.
        Long userId = currentUserProvider.getCurrentUserId();
        User user = userId == null ? null : userDAO.findById(userId);
        if (user == null) {
            throw new BadRequestException("Unable to determine the logged-in user performing the dispense");
        }
        prescription.setDispensed(true);
        prescription.setDispensedDate(LocalDateTime.now());
        prescription.setDispensedBy(user);
        Prescription saved = prescriptionDAO.save(prescription);
        auditLogService.logAction("Prescription", id, AuditAction.DISPENSE,
            user.getId(), currentUserProvider.getClientIp(),
            null, "prescriptionNumber=" + saved.getPrescriptionNumber());
        return saved;
    }

    // Indicates this method implements an interface contract
    @Override
    public void deletePrescription(Long id) {
        Prescription existing = prescriptionDAO.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Prescription", "id", id);
        }
        // A dispensed prescription has linked dispensing records - deleting it would orphan
        // them (FK constraint), so only pending, never-dispensed prescriptions can be removed.
        if (existing.isDispensed()) {
            throw new BadRequestException("A dispensed prescription cannot be deleted");
        }
        if (existing.isVoided()) {
            throw new BadRequestException("A voided prescription cannot be deleted");
        }
        prescriptionDAO.deleteById(id);
        auditLogService.logAction("Prescription", id, AuditAction.DELETE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            "id=" + id, null);
    }
}
