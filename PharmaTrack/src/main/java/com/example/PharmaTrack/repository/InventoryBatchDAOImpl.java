/*
 * ARCHITECTURE: Uses EntityManager directly (not Spring Data JpaRepository) to give full
 * control over JPQL queries. This matches the DAO pattern taught in class: the DAO interface
 * defines the contract, and this Impl class provides the persistence logic.
 */
package com.example.PharmaTrack.repository;

import com.example.PharmaTrack.dao.InventoryBatchDAO;
import com.example.PharmaTrack.entity.InventoryBatch;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class InventoryBatchDAOImpl implements InventoryBatchDAO {

    private EntityManager entityManager;

    @Autowired
    public InventoryBatchDAOImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public InventoryBatch findByBatchNumber(String batchNumber) {
        TypedQuery<InventoryBatch> query = entityManager.createQuery(
            "FROM InventoryBatch WHERE batchNumber = :batchNumber", InventoryBatch.class);
        query.setParameter("batchNumber", batchNumber);
        List<InventoryBatch> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<InventoryBatch> findByMedicineId(Long medicineId) {
        TypedQuery<InventoryBatch> query = entityManager.createQuery(
            "FROM InventoryBatch WHERE medicine.id = :medicineId", InventoryBatch.class);
        query.setParameter("medicineId", medicineId);
        return query.getResultList();
    }

    @Override
    public List<InventoryBatch> findBySupplierId(Long supplierId) {
        TypedQuery<InventoryBatch> query = entityManager.createQuery(
            "FROM InventoryBatch WHERE supplier.id = :supplierId", InventoryBatch.class);
        query.setParameter("supplierId", supplierId);
        return query.getResultList();
    }

    @Override
    public List<InventoryBatch> findByExpired(boolean expired) {
        TypedQuery<InventoryBatch> query = entityManager.createQuery(
            "FROM InventoryBatch WHERE expired = :expired", InventoryBatch.class);
        query.setParameter("expired", expired);
        return query.getResultList();
    }

    @Override
    public List<InventoryBatch> findExpiringBatches(LocalDate date) {
        TypedQuery<InventoryBatch> query = entityManager.createQuery(
            "SELECT ib FROM InventoryBatch ib WHERE ib.expiryDate <= :date AND ib.expired = false",
            InventoryBatch.class);
        query.setParameter("date", date);
        return query.getResultList();
    }

    @Override
    public List<InventoryBatch> findAvailableBatchesByMedicineId(Long medicineId) {
        TypedQuery<InventoryBatch> query = entityManager.createQuery(
            "SELECT ib FROM InventoryBatch ib WHERE ib.medicine.id = :medicineId " +
            "AND ib.quantityRemaining > 0 AND ib.expired = false ORDER BY ib.expiryDate ASC",
            InventoryBatch.class);
        query.setParameter("medicineId", medicineId);
        return query.getResultList();
    }

    @Override
    public InventoryBatch findById(Long id) {
        return entityManager.find(InventoryBatch.class, id);
    }

    @Override
    public List<InventoryBatch> findAll() {
        TypedQuery<InventoryBatch> query = entityManager.createQuery("FROM InventoryBatch", InventoryBatch.class);
        return query.getResultList();
    }

    @Override
    public InventoryBatch save(InventoryBatch inventoryBatch) {
        if (inventoryBatch.getId() == null) {
            entityManager.persist(inventoryBatch);
            return inventoryBatch;
        } else {
            return entityManager.merge(inventoryBatch);
        }
    }

    @Override
    public void deleteById(Long id) {
        InventoryBatch inventoryBatch = findById(id);
        if (inventoryBatch != null) {
            entityManager.remove(inventoryBatch);
        }
    }

    @Override
    public boolean existsById(Long id) {
        return findById(id) != null;
    }
}
