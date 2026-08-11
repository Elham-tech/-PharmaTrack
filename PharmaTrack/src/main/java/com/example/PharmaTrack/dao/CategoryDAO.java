package com.example.PharmaTrack.dao;

import com.example.PharmaTrack.entity.Category;

import java.util.List;

public interface CategoryDAO {
    Category findByName(String name);
    boolean existsByName(String name);
    Category findById(Long id);
    List<Category> findAll();
    Category save(Category category);
    void deleteById(Long id);
    boolean existsById(Long id);
}
