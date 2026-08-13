/*
 * ARCHITECTURE: Uses EntityManager directly (not Spring Data JpaRepository) to give full
 * control over JPQL queries. This matches the DAO pattern taught in class: the DAO interface
 * defines the contract, and this Impl class provides the persistence logic.
 */
package com.example.PharmaTrack.repository;

import com.example.PharmaTrack.dao.UserDAO;
import com.example.PharmaTrack.entity.User;
import com.example.PharmaTrack.entity.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserDAOImpl implements UserDAO {

    private EntityManager entityManager;

    @Autowired
    public UserDAOImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public User findByUsername(String username) {
        TypedQuery<User> query = entityManager.createQuery(
            "FROM User WHERE username = :username", User.class);
        query.setParameter("username", username);
        List<User> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public User findByUsernameWithAuthorities(String username) {
        // LEFT JOIN FETCH eagerly loads the authorities collection in the same query,
        // avoiding LazyInitializationException when Spring Security builds the UserDetails.
        TypedQuery<User> query = entityManager.createQuery(
            "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.authorities WHERE u.username = :username", User.class);
        query.setParameter("username", username);
        List<User> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public User findByEmail(String email) {
        TypedQuery<User> query = entityManager.createQuery(
            "FROM User WHERE email = :email", User.class);
        query.setParameter("email", email);
        List<User> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public boolean existsByUsername(String username) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class);
        query.setParameter("username", username);
        return query.getSingleResult() > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class);
        query.setParameter("email", email);
        return query.getSingleResult() > 0;
    }

    @Override
    public List<User> findByRole(Role role) {
        TypedQuery<User> query = entityManager.createQuery(
            "FROM User WHERE role = :role", User.class);
        query.setParameter("role", role);
        return query.getResultList();
    }

    @Override
    public List<User> findByActive(boolean active) {
        TypedQuery<User> query = entityManager.createQuery(
            "FROM User WHERE active = :active", User.class);
        query.setParameter("active", active);
        return query.getResultList();
    }

    @Override
    public User findById(Long id) {
        return entityManager.find(User.class, id);
    }

    @Override
    public List<User> findAll() {
        TypedQuery<User> query = entityManager.createQuery("FROM User", User.class);
        return query.getResultList();
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            entityManager.persist(user);
            return user;
        } else {
            return entityManager.merge(user);
        }
    }

    @Override
    public void deleteById(Long id) {
        User user = findById(id);
        if (user != null) {
            entityManager.remove(user);
        }
    }

    @Override
    public boolean existsById(Long id) {
        return findById(id) != null;
    }
}
