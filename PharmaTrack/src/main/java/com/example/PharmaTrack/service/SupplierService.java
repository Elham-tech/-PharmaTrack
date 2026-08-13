package com.example.PharmaTrack.service;

import com.example.PharmaTrack.entity.Supplier;

import java.util.List;

public interface SupplierService {
    Supplier createSupplier(Supplier supplier);
    Supplier updateSupplier(Long id, Supplier supplier);
    Supplier getSupplierById(Long id);
    Supplier getSupplierByCode(String code);
    List<Supplier> getAllSuppliers();
    List<Supplier> getActiveSuppliers();
    void deleteSupplier(Long id);
    boolean existsByCode(String code);
}
