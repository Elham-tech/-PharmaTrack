/*
 * ARCHITECTURE: Uses EntityManager directly (not Spring Data JpaRepository) to give full
 * control over JPQL queries. This matches the DAO pattern taught in class: the DAO interface
 * defines the contract, and this Impl class provides the persistence logic.
 */
package com.example.PharmaTrack.repository;

import com.example.PharmaTrack.dao.AuditLogDAO;
import com.example.PharmaTrack.entity.AuditLog;
import com.example.PharmaTrack.entity.AuditLog.AuditAction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AuditLogDAOImpl implements AuditLogDAO {

    private EntityManager entityManager;

    @Autowired
    public AuditLogDAOImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<AuditLog> findByEntityType(String entityType) {
        TypedQuery<AuditLog> query = entityManager.createQuery(
            "FROM AuditLog WHERE entityType = :entityType", AuditLog.class);
        query.setParameter("entityType", entityType);
        return query.getResultList();
    }

    @Override
    public List<AuditLog> findByEntityId(Long entityId) {
        TypedQuery<AuditLog> query = entityManager.createQuery(
            "FROM AuditLog WHERE entityId = :entityId", AuditLog.class);
        query.setParameter("entityId", entityId);
        return query.getResultList();
    }

    @Override
    public List<AuditLog> findByAction(AuditAction action) {
        TypedQuery<AuditLog> query = entityManager.createQuery(
            "FROM AuditLog WHERE action = :action", AuditLog.class);
        query.setParameter("action", action);
        return query.getResultList();
    }

    @Override
    public List<AuditLog> findByPerformedById(Long userId) {
        TypedQuery<AuditLog> query = entityManager.createQuery(
            "FROM AuditLog WHERE performedBy.id = :userId", AuditLog.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }

    @Override
    public List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end) {
        TypedQuery<AuditLog> query = entityManager.createQuery(
            "FROM AuditLog WHERE timestamp BETWEEN :start AND :end", AuditLog.class);
        query.setParameter("start", start);
        query.setParameter("end", end);
        return query.getResultList();
    }

    @Override
    public List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId) {
        TypedQuery<AuditLog> query = entityManager.createQuery(
            "FROM AuditLog WHERE entityType = :entityType AND entityId = :entityId", AuditLog.class);
        query.setParameter("entityType", entityType);
        query.setParameter("entityId", entityId);
        return query.getResultList();
    }

    @Override
    public AuditLog findById(Long id) {
        return entityManager.find(AuditLog.class, id);
    }

    @Override
    public List<AuditLog> findAll() {
        TypedQuery<AuditLog> query = entityManager.createQuery("FROM AuditLog", AuditLog.class);
        return query.getResultList();
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        if (auditLog.getId() == null) {
            entityManager.persist(auditLog);
            return auditLog;
        } else {
            return entityManager.merge(auditLog);
        }
    }
}
