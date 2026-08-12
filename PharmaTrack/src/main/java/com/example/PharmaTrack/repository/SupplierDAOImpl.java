/*
 * ARCHITECTURE: Uses EntityManager directly (not Spring Data JpaRepository) to give full
 * control over JPQL queries. This matches the DAO pattern taught in class: the DAO interface
 * defines the contract, and this Impl class provides the persistence logic.
 */
package com.example.PharmaTrack.repository;

import com.example.PharmaTrack.dao.SupplierDAO;
import com.example.PharmaTrack.entity.Supplier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SupplierDAOImpl implements SupplierDAO {

    private EntityManager entityManager;

    @Autowired
    public SupplierDAOImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Supplier findByCode(String code) {
        TypedQuery<Supplier> query = entityManager.createQuery(
            "FROM Supplier WHERE code = :code", Supplier.class);
        query.setParameter("code", code);
        List<Supplier> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public Supplier findByName(String name) {
        TypedQuery<Supplier> query = entityManager.createQuery(
            "FROM Supplier WHERE name = :name", Supplier.class);
        query.setParameter("name", name);
        List<Supplier> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public boolean existsByCode(String code) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(s) FROM Supplier s WHERE s.code = :code", Long.class);
        query.setParameter("code", code);
        return query.getSingleResult() > 0;
    }

    @Override
    public List<Supplier> findByActive(boolean active) {
        TypedQuery<Supplier> query = entityManager.createQuery(
            "FROM Supplier WHERE active = :active", Supplier.class);
        query.setParameter("active", active);
        return query.getResultList();
    }

    @Override
    public Supplier findById(Long id) {
        return entityManager.find(Supplier.class, id);
    }

    @Override
    public List<Supplier> findAll() {
        TypedQuery<Supplier> query = entityManager.createQuery("FROM Supplier", Supplier.class);
        return query.getResultList();
    }

    @Override
    public Supplier save(Supplier supplier) {
        if (supplier.getId() == null) {
            entityManager.persist(supplier);
            return supplier;
        } else {
            return entityManager.merge(supplier);
        }
    }

    @Override
    public void deleteById(Long id) {
        Supplier supplier = findById(id);
        if (supplier != null) {
            entityManager.remove(supplier);
        }
    }

    @Override
    public boolean existsById(Long id) {
        return findById(id) != null;
    }
}
