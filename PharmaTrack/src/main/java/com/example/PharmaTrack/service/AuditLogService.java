package com.example.PharmaTrack.service;

import com.example.PharmaTrack.entity.AuditLog;
import com.example.PharmaTrack.entity.AuditLog.AuditAction;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogService {
    AuditLog createAuditLog(AuditLog auditLog);
    AuditLog getAuditLogById(Long id);
    List<AuditLog> getAllAuditLogs();
    List<AuditLog> getAuditLogsByEntityType(String entityType);
    List<AuditLog> getAuditLogsByEntity(String entityType, Long entityId);
    List<AuditLog> getAuditLogsByAction(AuditAction action);
    List<AuditLog> getAuditLogsByUser(Long userId);
    List<AuditLog> getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end);
    void logAction(String entityType, Long entityId, AuditAction action, Long userId, String ipAddress, String oldValues, String newValues);
}
