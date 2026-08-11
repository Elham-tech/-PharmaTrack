/*
 * ARCHITECTURE: Service layer sits between controller and DAO. It owns all business rules
 * (duplicate checks, business validations) and transaction boundaries. @Transactional at
 * class level means every method runs inside a database transaction by default - if an
 * exception is thrown, all changes are rolled back. @Transactional(readOnly = true) on read
 * methods tells Hibernate to skip dirty checking and use a read-only connection, improving
 * performance for queries that don't modify data.
 */
package com.example.PharmaTrack.service.impl;

import com.example.PharmaTrack.entity.AuditLog.AuditAction;
import com.example.PharmaTrack.entity.Medicine;
import com.example.PharmaTrack.dao.MedicineDAO;
import com.example.PharmaTrack.security.CurrentUserProvider;
import com.example.PharmaTrack.service.AuditLogService;
import com.example.PharmaTrack.service.MedicineService;
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
public class MedicineServiceImpl implements MedicineService {

    private final MedicineDAO medicineDAO;
    private final AuditLogService auditLogService;
    private final CurrentUserProvider currentUserProvider;

    /*
     * ARCHITECTURE: Constructor injection (not field injection) makes dependencies explicit
     * and the class testable - you can pass mock DAOs in tests. 'final' fields ensure the
     * service is never in a partially-constructed state. Spring auto-wires this constructor
     * because there's only one constructor (no @Autowired needed in Spring 4.3+, but we keep
     * it for clarity).
     */
    @Autowired
    public MedicineServiceImpl(MedicineDAO medicineDAO,
                               AuditLogService auditLogService,
                               CurrentUserProvider currentUserProvider) {
        this.medicineDAO = medicineDAO;
        this.auditLogService = auditLogService;
        this.currentUserProvider = currentUserProvider;
    }

    // Indicates this method implements an interface contract
    @Override
    public Medicine createMedicine(Medicine medicine) {
        if (medicineDAO.findByCode(medicine.getCode()) != null) {
            throw new BadRequestException("Medicine code already exists: " + medicine.getCode());
        }
        Medicine created = medicineDAO.save(medicine);
        auditLogService.logAction("Medicine", created.getId(), AuditAction.CREATE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            null, "code=" + created.getCode() + ", name=" + created.getName());
        return created;
    }

    // Indicates this method implements an interface contract
    @Override
    public Medicine updateMedicine(Long id, Medicine medicine) {
        Medicine existingMedicine = medicineDAO.findById(id);
        if (existingMedicine == null) {
            throw new ResourceNotFoundException("Medicine", "id", id);
        }
        String oldValues = "code=" + existingMedicine.getCode() + ", name=" + existingMedicine.getName();
        existingMedicine.setCode(medicine.getCode());
        existingMedicine.setName(medicine.getName());
        existingMedicine.setDescription(medicine.getDescription());
        existingMedicine.setCategory(medicine.getCategory());
        existingMedicine.setManufacturer(medicine.getManufacturer());
        existingMedicine.setUnit(medicine.getUnit());
        existingMedicine.setRequiresPrescription(medicine.isRequiresPrescription());
        Medicine saved = medicineDAO.save(existingMedicine);
        auditLogService.logAction("Medicine", id, AuditAction.UPDATE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            oldValues, "code=" + saved.getCode() + ", name=" + saved.getName());
        return saved;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public Medicine getMedicineById(Long id) {
        Medicine medicine = medicineDAO.findById(id);
        if (medicine == null) {
            throw new ResourceNotFoundException("Medicine", "id", id);
        }
        return medicine;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public Medicine getMedicineByCode(String code) {
        Medicine medicine = medicineDAO.findByCode(code);
        if (medicine == null) {
            throw new ResourceNotFoundException("Medicine", "code", code);
        }
        return medicine;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<Medicine> getAllMedicines() {
        return medicineDAO.findAll();
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<Medicine> getMedicinesByCategory(Long categoryId) {
        return medicineDAO.findByCategoryId(categoryId);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<Medicine> getMedicinesByManufacturer(Long manufacturerId) {
        return medicineDAO.findByManufacturerId(manufacturerId);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<Medicine> searchMedicinesByName(String name) {
        return medicineDAO.searchByName(name);
    }

    // Indicates this method implements an interface contract
    @Override
    public void deleteMedicine(Long id) {
        if (!medicineDAO.existsById(id)) {
            throw new ResourceNotFoundException("Medicine", "id", id);
        }
        medicineDAO.deleteById(id);
        auditLogService.logAction("Medicine", id, AuditAction.DELETE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            "id=" + id, null);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        return medicineDAO.findByCode(code) != null;
    }
}
