/*
 * ARCHITECTURE: Service layer sits between controller and DAO. It owns all business rules
 * (duplicate checks, business validations) and transaction boundaries. @Transactional at
 * class level means every method runs inside a database transaction by default - if an
 * exception is thrown, all changes are rolled back.
 */
package com.example.PharmaTrack.service.impl;

import com.example.PharmaTrack.entity.AuditLog.AuditAction;
import com.example.PharmaTrack.entity.Category;
import com.example.PharmaTrack.dao.CategoryDAO;
import com.example.PharmaTrack.security.CurrentUserProvider;
import com.example.PharmaTrack.service.AuditLogService;
import com.example.PharmaTrack.service.CategoryService;
import com.example.PharmaTrack.exception.ResourceNotFoundException;
import com.example.PharmaTrack.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Marks this class as a Spring-managed service (business logic layer)
@Service
// Makes all methods transactional by default (auto-commits or rolls back on exception)
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryDAO categoryDAO;
    private final AuditLogService auditLogService;
    private final CurrentUserProvider currentUserProvider;

    @Autowired
    public CategoryServiceImpl(CategoryDAO categoryDAO,
                               AuditLogService auditLogService,
                               CurrentUserProvider currentUserProvider) {
        this.categoryDAO = categoryDAO;
        this.auditLogService = auditLogService;
        this.currentUserProvider = currentUserProvider;
    }

    // Indicates this method implements an interface contract
    @Override
    public Category createCategory(Category category) {
        if (categoryDAO.existsByName(category.getName())) {
            throw new BadRequestException("Category name already exists: " + category.getName());
        }
        Category created = categoryDAO.save(category);
        auditLogService.logAction("Category", created.getId(), AuditAction.CREATE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            null, "name=" + created.getName());
        return created;
    }

    // Indicates this method implements an interface contract
    @Override
    public Category updateCategory(Long id, Category category) {
        Category existingCategory = categoryDAO.findById(id);
        if (existingCategory == null) {
            throw new ResourceNotFoundException("Category", "id", id);
        }
        String oldValues = "name=" + existingCategory.getName();
        existingCategory.setName(category.getName());
        existingCategory.setDescription(category.getDescription());
        Category saved = categoryDAO.save(existingCategory);
        auditLogService.logAction("Category", id, AuditAction.UPDATE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            oldValues, "name=" + saved.getName());
        return saved;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public Category getCategoryById(Long id) {
        Category category = categoryDAO.findById(id);
        if (category == null) {
            throw new ResourceNotFoundException("Category", "id", id);
        }
        return category;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public Category getCategoryByName(String name) {
        Category category = categoryDAO.findByName(name);
        if (category == null) {
            throw new ResourceNotFoundException("Category", "name", name);
        }
        return category;
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryDAO.findAll();
    }

    // Indicates this method implements an interface contract
    @Override
    public void deleteCategory(Long id) {
        if (!categoryDAO.existsById(id)) {
            throw new ResourceNotFoundException("Category", "id", id);
        }
        categoryDAO.deleteById(id);
        auditLogService.logAction("Category", id, AuditAction.DELETE,
            currentUserProvider.getCurrentUserId(), currentUserProvider.getClientIp(),
            "id=" + id, null);
    }

    // Indicates this method implements an interface contract
    @Override
    // Optimizes read-only queries (no write lock, potential replica routing)
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return categoryDAO.existsByName(name);
    }
}
