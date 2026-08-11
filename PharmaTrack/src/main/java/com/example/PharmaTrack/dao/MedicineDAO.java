/*
 * ARCHITECTURE: Pure interface with no framework annotations. The service layer depends
 * on this contract, not on the concrete implementation. This allows swapping the persistence
 * mechanism (e.g. switching from EntityManager to JDBC) without changing any service code.
 * Also makes unit testing easier by allowing mock implementations.
 */
package com.example.PharmaTrack.dao;

import com.example.PharmaTrack.entity.Medicine;

import java.util.List;

public interface MedicineDAO {
    Medicine findByCode(String code);
    Medicine findByName(String name);
    List<Medicine> findByCategoryId(Long categoryId);
    List<Medicine> findByManufacturerId(Long manufacturerId);
    List<Medicine> findByRequiresPrescription(boolean requiresPrescription);
    List<Medicine> findByActive(boolean active);
    List<Medicine> searchByName(String name);
    Medicine findById(Long id);
    List<Medicine> findAll();
    Medicine save(Medicine medicine);
    void deleteById(Long id);
    boolean existsById(Long id);
}
