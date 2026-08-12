/*
 * ARCHITECTURE: Uses EntityManager directly (not Spring Data JpaRepository) to give full
 * control over JPQL queries. This matches the DAO pattern taught in class: the DAO interface
 * defines the contract, and this Impl class provides the persistence logic.
 */
package com.example.PharmaTrack.repository;

import com.example.PharmaTrack.dao.DispensingRecordDAO;
import com.example.PharmaTrack.entity.DispensingRecord;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DispensingRecordDAOImpl implements DispensingRecordDAO {

    private EntityManager entityManager;

    @Autowired
    public DispensingRecordDAOImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public DispensingRecord findByDispensingNumber(String dispensingNumber) {
        TypedQuery<DispensingRecord> query = entityManager.createQuery(
            "FROM DispensingRecord WHERE dispensingNumber = :dispensingNumber", DispensingRecord.class);
        query.setParameter("dispensingNumber", dispensingNumber);
        List<DispensingRecord> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<DispensingRecord> findByPrescriptionId(Long prescriptionId) {
        TypedQuery<DispensingRecord> query = entityManager.createQuery(
            "FROM DispensingRecord WHERE prescription.id = :prescriptionId", DispensingRecord.class);
        query.setParameter("prescriptionId", prescriptionId);
        return query.getResultList();
    }

    @Override
    public List<DispensingRecord> findByMedicineId(Long medicineId) {
        TypedQuery<DispensingRecord> query = entityManager.createQuery(
            "FROM DispensingRecord WHERE medicine.id = :medicineId", DispensingRecord.class);
        query.setParameter("medicineId", medicineId);
        return query.getResultList();
    }

    @Override
    public List<DispensingRecord> findByDispensedById(Long userId) {
        TypedQuery<DispensingRecord> query = entityManager.createQuery(
            "FROM DispensingRecord WHERE dispensedBy.id = :userId", DispensingRecord.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }

    @Override
    public DispensingRecord findById(Long id) {
        return entityManager.find(DispensingRecord.class, id);
    }

    @Override
    public List<DispensingRecord> findAll() {
        TypedQuery<DispensingRecord> query = entityManager.createQuery("FROM DispensingRecord", DispensingRecord.class);
        return query.getResultList();
    }

    @Override
    public DispensingRecord save(DispensingRecord dispensingRecord) {
        if (dispensingRecord.getId() == null) {
            entityManager.persist(dispensingRecord);
            return dispensingRecord;
        } else {
            return entityManager.merge(dispensingRecord);
        }
    }

    @Override
    public void deleteById(Long id) {
        DispensingRecord dispensingRecord = findById(id);
        if (dispensingRecord != null) {
            entityManager.remove(dispensingRecord);
        }
    }

    @Override
    public boolean existsById(Long id) {
        return findById(id) != null;
    }
}
