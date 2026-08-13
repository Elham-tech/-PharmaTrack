/*
 * ARCHITECTURE: Uses EntityManager directly (not Spring Data JpaRepository) to give full
 * control over JPQL queries. This matches the DAO pattern taught in class: the DAO interface
 * defines the contract, and this Impl class provides the persistence logic.
 */
package com.example.PharmaTrack.repository;

import com.example.PharmaTrack.dao.AuthorityDAO;
import com.example.PharmaTrack.entity.Authority;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AuthorityDAOImpl implements AuthorityDAO {

    private EntityManager entityManager;

    @Autowired
    public AuthorityDAOImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Authority findByName(String name) {
        TypedQuery<Authority> query = entityManager.createQuery(
            "FROM Authority WHERE name = :name", Authority.class);
        query.setParameter("name", name);
        List<Authority> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public Authority findById(Long id) {
        return entityManager.find(Authority.class, id);
    }

    @Override
    public List<Authority> findAll() {
        TypedQuery<Authority> query = entityManager.createQuery("FROM Authority", Authority.class);
        return query.getResultList();
    }

    @Override
    public Authority save(Authority authority) {
        if (authority.getId() == null) {
            entityManager.persist(authority);
            return authority;
        } else {
            return entityManager.merge(authority);
        }
    }
}
