package com.bookapp.controller;

import com.bookapp.entity.Category;
import com.bookapp.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<Category> getAll(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return categoryService.findByUser(userId);
    }

    @PostMapping
    public Category create(Authentication auth, @RequestBody CategoryService.CategoryRequest req) {
        Long userId = (Long) auth.getPrincipal();
        return categoryService.create(userId, req);
    }

    @PutMapping("/{id}")
    public Category update(Authentication auth, @PathVariable Long id,
                            @RequestBody CategoryService.CategoryRequest req) {
        Long userId = (Long) auth.getPrincipal();
        return categoryService.update(userId, id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        try {
            categoryService.delete(userId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
