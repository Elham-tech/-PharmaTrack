package com.example.PharmaTrack.repository;

import com.example.PharmaTrack.dao.MedicineDAO;
import com.example.PharmaTrack.entity.Medicine;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/*
 * ARCHITECTURE: Uses EntityManager directly (not Spring Data JpaRepository) to give full
 * control over JPQL queries. This matches the DAO pattern taught in class: the DAO interface
 * defines the contract, and this Impl class provides the persistence logic. EntityManager
 * is injected by Spring's JPA infrastructure and manages the persistence context (session).
 */
@Repository
public class MedicineDAOImpl implements MedicineDAO {

    private EntityManager entityManager;

    @Autowired
    public MedicineDAOImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Medicine findByCode(String code) {
        TypedQuery<Medicine> query = entityManager.createQuery(
            "FROM Medicine WHERE code = :code", Medicine.class);
        query.setParameter("code", code);
        List<Medicine> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public Medicine findByName(String name) {
        TypedQuery<Medicine> query = entityManager.createQuery(
            "FROM Medicine WHERE name = :name", Medicine.class);
        query.setParameter("name", name);
        List<Medicine> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<Medicine> findByCategoryId(Long categoryId) {
        TypedQuery<Medicine> query = entityManager.createQuery(
            "FROM Medicine WHERE category.id = :categoryId", Medicine.class);
        query.setParameter("categoryId", categoryId);
        return query.getResultList();
    }

    @Override
    public List<Medicine> findByManufacturerId(Long manufacturerId) {
        TypedQuery<Medicine> query = entityManager.createQuery(
            "FROM Medicine WHERE manufacturer.id = :manufacturerId", Medicine.class);
        query.setParameter("manufacturerId", manufacturerId);
        return query.getResultList();
    }

    @Override
    public List<Medicine> findByRequiresPrescription(boolean requiresPrescription) {
        TypedQuery<Medicine> query = entityManager.createQuery(
            "FROM Medicine WHERE requiresPrescription = :requiresPrescription", Medicine.class);
        query.setParameter("requiresPrescription", requiresPrescription);
        return query.getResultList();
    }

    @Override
    public List<Medicine> findByActive(boolean active) {
        TypedQuery<Medicine> query = entityManager.createQuery(
            "FROM Medicine WHERE active = :active", Medicine.class);
        query.setParameter("active", active);
        return query.getResultList();
    }

    @Override
    public List<Medicine> searchByName(String name) {
        TypedQuery<Medicine> query = entityManager.createQuery(
            "FROM Medicine WHERE LOWER(name) LIKE LOWER(CONCAT('%', :name, '%'))", Medicine.class);
        query.setParameter("name", name);
        return query.getResultList();
    }

    @Override
    public Medicine findById(Long id) {
        return entityManager.find(Medicine.class, id);
    }

    @Override
    public List<Medicine> findAll() {
        TypedQuery<Medicine> query = entityManager.createQuery("FROM Medicine", Medicine.class);
        return query.getResultList();
    }

    @Override
    public Medicine save(Medicine medicine) {
        if (medicine.getId() == null) {
            entityManager.persist(medicine);
            return medicine;
        } else {
            return entityManager.merge(medicine);
        }
    }

    @Override
    public void deleteById(Long id) {
        Medicine medicine = findById(id);
        if (medicine != null) {
            entityManager.remove(medicine);
        }
    }

    @Override
    public boolean existsById(Long id) {
        return findById(id) != null;
    }
}
