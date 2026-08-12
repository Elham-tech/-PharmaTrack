package com.example.PharmaTrack.dao;

import com.example.PharmaTrack.entity.AuditLog;
import com.example.PharmaTrack.entity.AuditLog.AuditAction;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogDAO {
    List<AuditLog> findByEntityType(String entityType);
    List<AuditLog> findByEntityId(Long entityId);
    List<AuditLog> findByAction(AuditAction action);
    List<AuditLog> findByPerformedById(Long userId);
    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);
    AuditLog findById(Long id);
    List<AuditLog> findAll();
    AuditLog save(AuditLog auditLog);
}
