package com.example.PharmaTrack.dao;

import com.example.PharmaTrack.entity.Manufacturer;

import java.util.List;

public interface ManufacturerDAO {
    Manufacturer findByName(String name);
    boolean existsByName(String name);
    Manufacturer findById(Long id);
    List<Manufacturer> findAll();
    Manufacturer save(Manufacturer manufacturer);
    void deleteById(Long id);
    boolean existsById(Long id);
}
