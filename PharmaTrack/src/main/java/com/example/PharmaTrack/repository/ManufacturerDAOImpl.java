/*
 * ARCHITECTURE: Uses EntityManager directly (not Spring Data JpaRepository) to give full
 * control over JPQL queries. This matches the DAO pattern taught in class: the DAO interface
 * defines the contract, and this Impl class provides the persistence logic.
 */
package com.example.PharmaTrack.repository;

import com.example.PharmaTrack.dao.ManufacturerDAO;
import com.example.PharmaTrack.entity.Manufacturer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ManufacturerDAOImpl implements ManufacturerDAO {

    private EntityManager entityManager;

    @Autowired
    public ManufacturerDAOImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Manufacturer findByName(String name) {
        TypedQuery<Manufacturer> query = entityManager.createQuery(
            "FROM Manufacturer WHERE name = :name", Manufacturer.class);
        query.setParameter("name", name);
        List<Manufacturer> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public boolean existsByName(String name) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(m) FROM Manufacturer m WHERE m.name = :name", Long.class);
        query.setParameter("name", name);
        return query.getSingleResult() > 0;
    }

    @Override
    public Manufacturer findById(Long id) {
        return entityManager.find(Manufacturer.class, id);
    }

    @Override
    public List<Manufacturer> findAll() {
        TypedQuery<Manufacturer> query = entityManager.createQuery("FROM Manufacturer", Manufacturer.class);
        return query.getResultList();
    }

    @Override
    public Manufacturer save(Manufacturer manufacturer) {
        if (manufacturer.getId() == null) {
            entityManager.persist(manufacturer);
            return manufacturer;
        } else {
            return entityManager.merge(manufacturer);
        }
    }

    @Override
    public void deleteById(Long id) {
        Manufacturer manufacturer = findById(id);
        if (manufacturer != null) {
            entityManager.remove(manufacturer);
        }
    }

    @Override
    public boolean existsById(Long id) {
        return findById(id) != null;
    }
}
