/*
 * ARCHITECTURE: Service layer sits between controller and DAO. It owns all business rules
 * (duplicate checks, business validations) and transaction boundaries. @Transactional at
 * class level means every method runs inside a database transaction by default - if an
 * exception is thrown, all changes are rolled back.
 */
package com.example.PharmaTrack.service.impl;

import com.example.PharmaTrack.entity.AuditLog;
import com.example.PharmaTrack.entity.AuditLog.AuditAction;
import com.example.PharmaTrack.entity.User;
import com.example.PharmaTrack.dao.AuditLogDAO;
import com.example.PharmaTrack.dao.UserDAO;
import com.example.PharmaTrack.service.AuditLogService;
import com.example.PharmaTrack.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Marks this class as a Spring-managed service (business logic layer)
@Service
// Makes all methods transactional by default (auto-commits or rolls back on exception)
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogDAO auditLogDAO;
    private final UserDAO userDAO;

    @Autowired
    public AuditLogServiceImpl(AuditLogDAO auditLogDAO,
                                UserDAO userDAO) {
        this.auditLogDAO = auditLogDAO;
        this.userDAO = userDAO;
    }

    // Indicates this method implements an interface contract
    @Override
    public AuditLog createAuditLog(AuditLog auditLog) {
        return auditLogDAO.save(auditLog);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public AuditLog getAuditLogById(Long id) {
        AuditLog auditLog = auditLogDAO.findById(id);
        if (auditLog == null) {
            throw new ResourceNotFoundException("AuditLog", "id", id);
        }
        return auditLog;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<AuditLog> getAllAuditLogs() {
        return auditLogDAO.findAll();
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByEntityType(String entityType) {
        return auditLogDAO.findByEntityType(entityType);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByEntity(String entityType, Long entityId) {
        return auditLogDAO.findByEntityTypeAndEntityId(entityType, entityId);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByAction(AuditAction action) {
        return auditLogDAO.findByAction(action);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByUser(Long userId) {
        return auditLogDAO.findByPerformedById(userId);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogDAO.findByTimestampBetween(start, end);
    }

    // Indicates this method implements an interface contract
    @Override
    public void logAction(String entityType, Long entityId, AuditAction action, Long userId, String ipAddress, String oldValues, String newValues) {
        // No authenticated user (e.g. system-triggered operation): nothing meaningful to audit.
        if (userId == null) {
            return;
        }
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        AuditLog auditLog = new AuditLog(entityType, entityId, action, user, ipAddress);
        auditLog.setOldValues(oldValues);
        auditLog.setNewValues(newValues);
        auditLogDAO.save(auditLog);
    }
}
