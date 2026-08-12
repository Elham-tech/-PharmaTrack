package com.example.PharmaTrack.dao;

import com.example.PharmaTrack.entity.DispensingRecord;

import java.util.List;

public interface DispensingRecordDAO {
    DispensingRecord findByDispensingNumber(String dispensingNumber);
    List<DispensingRecord> findByPrescriptionId(Long prescriptionId);
    List<DispensingRecord> findByMedicineId(Long medicineId);
    List<DispensingRecord> findByDispensedById(Long userId);
    DispensingRecord findById(Long id);
    List<DispensingRecord> findAll();
    DispensingRecord save(DispensingRecord dispensingRecord);
    void deleteById(Long id);
    boolean existsById(Long id);
}
