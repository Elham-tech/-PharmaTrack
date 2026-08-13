/*
 * ARCHITECTURE: Uses EntityManager directly (not Spring Data JpaRepository) to give full
 * control over JPQL queries. This matches the DAO pattern taught in class: the DAO interface
 * defines the contract, and this Impl class provides the persistence logic.
 */
package com.example.PharmaTrack.repository;

import com.example.PharmaTrack.dao.StockMovementDAO;
import com.example.PharmaTrack.entity.StockMovement;
import com.example.PharmaTrack.entity.StockMovement.MovementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StockMovementDAOImpl implements StockMovementDAO {

    private EntityManager entityManager;

    @Autowired
    public StockMovementDAOImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<StockMovement> findByMovementType(MovementType movementType) {
        TypedQuery<StockMovement> query = entityManager.createQuery(
            "FROM StockMovement WHERE movementType = :movementType", StockMovement.class);
        query.setParameter("movementType", movementType);
        return query.getResultList();
    }

    @Override
    public List<StockMovement> findByMedicineId(Long medicineId) {
        TypedQuery<StockMovement> query = entityManager.createQuery(
            "FROM StockMovement WHERE medicine.id = :medicineId", StockMovement.class);
        query.setParameter("medicineId", medicineId);
        return query.getResultList();
    }

    @Override
    public List<StockMovement> findByInventoryBatchId(Long inventoryBatchId) {
        TypedQuery<StockMovement> query = entityManager.createQuery(
            "FROM StockMovement WHERE inventoryBatch.id = :inventoryBatchId", StockMovement.class);
        query.setParameter("inventoryBatchId", inventoryBatchId);
        return query.getResultList();
    }

    @Override
    public List<StockMovement> findByPerformedById(Long userId) {
        TypedQuery<StockMovement> query = entityManager.createQuery(
            "FROM StockMovement WHERE performedBy.id = :userId", StockMovement.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }

    @Override
    public StockMovement findById(Long id) {
        return entityManager.find(StockMovement.class, id);
    }

    @Override
    public List<StockMovement> findAll() {
        TypedQuery<StockMovement> query = entityManager.createQuery("FROM StockMovement", StockMovement.class);
        return query.getResultList();
    }

    @Override
    public StockMovement save(StockMovement stockMovement) {
        if (stockMovement.getId() == null) {
            entityManager.persist(stockMovement);
            return stockMovement;
        } else {
            return entityManager.merge(stockMovement);
        }
    }
}
