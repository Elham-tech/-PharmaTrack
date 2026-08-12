package com.example.PharmaTrack.service;

import com.example.PharmaTrack.entity.Manufacturer;

import java.util.List;

public interface ManufacturerService {
    Manufacturer createManufacturer(Manufacturer manufacturer);
    Manufacturer updateManufacturer(Long id, Manufacturer manufacturer);
    Manufacturer getManufacturerById(Long id);
    Manufacturer getManufacturerByName(String name);
    List<Manufacturer> getAllManufacturers();
    void deleteManufacturer(Long id);
    boolean existsByName(String name);
}
