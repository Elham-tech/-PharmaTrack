package com.example.PharmaTrack.dao;

import com.example.PharmaTrack.entity.Supplier;

import java.util.List;

public interface SupplierDAO {
    Supplier findByCode(String code);
    Supplier findByName(String name);
    boolean existsByCode(String code);
    List<Supplier> findByActive(boolean active);
    Supplier findById(Long id);
    List<Supplier> findAll();
    Supplier save(Supplier supplier);
    void deleteById(Long id);
    boolean existsById(Long id);
}
