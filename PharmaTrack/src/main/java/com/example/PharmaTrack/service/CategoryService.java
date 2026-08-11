package com.example.PharmaTrack.service;

import com.example.PharmaTrack.entity.Category;

import java.util.List;

public interface CategoryService {
    Category createCategory(Category category);
    Category updateCategory(Long id, Category category);
    Category getCategoryById(Long id);
    Category getCategoryByName(String name);
    List<Category> getAllCategories();
    void deleteCategory(Long id);
    boolean existsByName(String name);
}
