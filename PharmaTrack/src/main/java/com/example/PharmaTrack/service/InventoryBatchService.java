package com.example.PharmaTrack.service;

import com.example.PharmaTrack.entity.InventoryBatch;

import java.time.LocalDate;
import java.util.List;

public interface InventoryBatchService {
    InventoryBatch createInventoryBatch(InventoryBatch inventoryBatch);
    InventoryBatch updateInventoryBatch(Long id, InventoryBatch inventoryBatch);
    InventoryBatch getInventoryBatchById(Long id);
    InventoryBatch getInventoryBatchByNumber(String batchNumber);
    List<InventoryBatch> getAllInventoryBatches();
    List<InventoryBatch> getBatchesByMedicine(Long medicineId);
    List<InventoryBatch> getBatchesBySupplier(Long supplierId);
    List<InventoryBatch> getExpiringBatches(LocalDate expiryDate);
    List<InventoryBatch> getAvailableBatchesByMedicine(Long medicineId);
    void deleteInventoryBatch(Long id);
}
