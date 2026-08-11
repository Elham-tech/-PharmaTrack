/*
 * ARCHITECTURE: Interface-first design decouples the controller from the implementation.
 * The controller and tests depend on this interface, not the concrete class. This allows
 * swapping implementations (e.g. for testing with a mock service) without changing callers.
 */
package com.example.PharmaTrack.service;

import com.example.PharmaTrack.entity.Medicine;

import java.util.List;

public interface MedicineService {
    Medicine createMedicine(Medicine medicine);
    Medicine updateMedicine(Long id, Medicine medicine);
    Medicine getMedicineById(Long id);
    Medicine getMedicineByCode(String code);
    List<Medicine> getAllMedicines();
    List<Medicine> getMedicinesByCategory(Long categoryId);
    List<Medicine> getMedicinesByManufacturer(Long manufacturerId);
    List<Medicine> searchMedicinesByName(String name);
    void deleteMedicine(Long id);
    boolean existsByCode(String code);
}
