package com.example.PharmaTrack.service;

import com.example.PharmaTrack.dto.StockMovementRequest;
import com.example.PharmaTrack.entity.StockMovement;
import com.example.PharmaTrack.entity.StockMovement.MovementType;

import java.util.List;

public interface StockMovementService {
    StockMovement createStockMovement(StockMovement stockMovement);
    StockMovement getStockMovementById(Long id);
    List<StockMovement> getAllStockMovements();
    List<StockMovement> getStockMovementsByType(MovementType movementType);
    List<StockMovement> getStockMovementsByMedicine(Long medicineId);
    List<StockMovement> getStockMovementsByBatch(Long batchId);
    List<StockMovement> getStockMovementsByUser(Long userId);
    void processStockIn(StockMovementRequest request);
    void processStockOut(StockMovementRequest request);
}
