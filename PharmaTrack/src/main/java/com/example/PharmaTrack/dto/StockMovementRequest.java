package com.example.PharmaTrack.dto;

import jakarta.validation.constraints.*;

public class StockMovementRequest {

    @NotNull(message = "Medicine ID is required")
    private Long medicineId;

    @NotNull(message = "Inventory batch ID is required")
    private Long inventoryBatchId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private int quantity;

    private String referenceNumber;

    private String notes;

    public StockMovementRequest() {}

    public Long getMedicineId() { return medicineId; }
    public void setMedicineId(Long medicineId) { this.medicineId = medicineId; }
    public Long getInventoryBatchId() { return inventoryBatchId; }
    public void setInventoryBatchId(Long inventoryBatchId) { this.inventoryBatchId = inventoryBatchId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
