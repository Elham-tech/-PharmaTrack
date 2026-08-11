package com.example.PharmaTrack.dao;

import com.example.PharmaTrack.entity.InventoryBatch;

import java.time.LocalDate;
import java.util.List;

public interface InventoryBatchDAO {
    InventoryBatch findByBatchNumber(String batchNumber);
    List<InventoryBatch> findByMedicineId(Long medicineId);
    List<InventoryBatch> findBySupplierId(Long supplierId);
    List<InventoryBatch> findByExpired(boolean expired);
    List<InventoryBatch> findExpiringBatches(LocalDate date);
    List<InventoryBatch> findAvailableBatchesByMedicineId(Long medicineId);
    InventoryBatch findById(Long id);
    List<InventoryBatch> findAll();
    InventoryBatch save(InventoryBatch inventoryBatch);
    void deleteById(Long id);
    boolean existsById(Long id);
}
