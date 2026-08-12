package com.example.PharmaTrack.service;

import com.example.PharmaTrack.entity.DispensingRecord;

import java.util.List;

public interface DispensingRecordService {
    DispensingRecord createDispensingRecord(DispensingRecord dispensingRecord);
    DispensingRecord getDispensingRecordById(Long id);
    DispensingRecord getDispensingRecordByNumber(String dispensingNumber);
    List<DispensingRecord> getAllDispensingRecords();
    List<DispensingRecord> getDispensingRecordsByPrescription(Long prescriptionId);
    List<DispensingRecord> getDispensingRecordsByMedicine(Long medicineId);
    List<DispensingRecord> getDispensingRecordsByUser(Long userId);
    DispensingRecord approvePayment(Long id);
    DispensingRecord voidDispensing(Long id);
}
