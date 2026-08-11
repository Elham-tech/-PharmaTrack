package com.example.PharmaTrack.dao;

import com.example.PharmaTrack.entity.Prescription;

import java.util.List;

public interface PrescriptionDAO {
    Prescription findByPrescriptionNumber(String prescriptionNumber);
    List<Prescription> findByPatientName(String patientName);
    List<Prescription> findByDispensed(boolean dispensed);
    List<Prescription> findByDoctorName(String doctorName);
    Prescription findById(Long id);
    List<Prescription> findAll();
    Prescription save(Prescription prescription);
    void deleteById(Long id);
    boolean existsById(Long id);
}
