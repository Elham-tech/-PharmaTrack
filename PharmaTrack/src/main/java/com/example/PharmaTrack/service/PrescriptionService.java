package com.example.PharmaTrack.service;

import com.example.PharmaTrack.entity.Prescription;

import java.util.List;

public interface PrescriptionService {
    Prescription createPrescription(Prescription prescription);
    Prescription updatePrescription(Long id, Prescription prescription);
    Prescription getPrescriptionById(Long id);
    Prescription getPrescriptionByNumber(String prescriptionNumber);
    List<Prescription> getAllPrescriptions();
    List<Prescription> getPrescriptionsByPatient(String patientName);
    List<Prescription> getPrescriptionsByDoctor(String doctorName);
    List<Prescription> getUnDispensedPrescriptions();
    Prescription markAsDispensed(Long id);
    void deletePrescription(Long id);
}
