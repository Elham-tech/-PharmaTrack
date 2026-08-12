/*
 * ARCHITECTURE: Uses EntityManager directly (not Spring Data JpaRepository) to give full
 * control over JPQL queries. This matches the DAO pattern taught in class: the DAO interface
 * defines the contract, and this Impl class provides the persistence logic.
 */
package com.example.PharmaTrack.repository;

import com.example.PharmaTrack.dao.PrescriptionDAO;
import com.example.PharmaTrack.entity.Prescription;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PrescriptionDAOImpl implements PrescriptionDAO {

    private EntityManager entityManager;

    @Autowired
    public PrescriptionDAOImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Prescription findByPrescriptionNumber(String prescriptionNumber) {
        TypedQuery<Prescription> query = entityManager.createQuery(
            "FROM Prescription WHERE prescriptionNumber = :prescriptionNumber", Prescription.class);
        query.setParameter("prescriptionNumber", prescriptionNumber);
        List<Prescription> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<Prescription> findByPatientName(String patientName) {
        TypedQuery<Prescription> query = entityManager.createQuery(
            "FROM Prescription WHERE patientName = :patientName", Prescription.class);
        query.setParameter("patientName", patientName);
        return query.getResultList();
    }

    @Override
    public List<Prescription> findByDispensed(boolean dispensed) {
        TypedQuery<Prescription> query = entityManager.createQuery(
            "FROM Prescription WHERE dispensed = :dispensed AND voided = false", Prescription.class);
        query.setParameter("dispensed", dispensed);
        return query.getResultList();
    }

    @Override
    public List<Prescription> findByDoctorName(String doctorName) {
        TypedQuery<Prescription> query = entityManager.createQuery(
            "FROM Prescription WHERE doctorName = :doctorName", Prescription.class);
        query.setParameter("doctorName", doctorName);
        return query.getResultList();
    }

    @Override
    public Prescription findById(Long id) {
        return entityManager.find(Prescription.class, id);
    }

    @Override
    public List<Prescription> findAll() {
        TypedQuery<Prescription> query = entityManager.createQuery("FROM Prescription", Prescription.class);
        return query.getResultList();
    }

    @Override
    public Prescription save(Prescription prescription) {
        if (prescription.getId() == null) {
            entityManager.persist(prescription);
            return prescription;
        } else {
            return entityManager.merge(prescription);
        }
    }

    @Override
    public void deleteById(Long id) {
        Prescription prescription = findById(id);
        if (prescription != null) {
            entityManager.remove(prescription);
        }
    }

    @Override
    public boolean existsById(Long id) {
        return findById(id) != null;
    }
}
