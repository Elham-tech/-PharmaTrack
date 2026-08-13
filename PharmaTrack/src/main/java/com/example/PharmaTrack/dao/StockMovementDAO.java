package com.example.PharmaTrack.dao;

import com.example.PharmaTrack.entity.StockMovement;
import com.example.PharmaTrack.entity.StockMovement.MovementType;

import java.util.List;

public interface StockMovementDAO {
    List<StockMovement> findByMovementType(MovementType movementType);
    List<StockMovement> findByMedicineId(Long medicineId);
    List<StockMovement> findByInventoryBatchId(Long inventoryBatchId);
    List<StockMovement> findByPerformedById(Long userId);
    StockMovement findById(Long id);
    List<StockMovement> findAll();
    StockMovement save(StockMovement stockMovement);
}
