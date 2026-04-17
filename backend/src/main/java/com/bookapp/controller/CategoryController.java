package com.bookapp.controller;

import com.bookapp.entity.Category;
import com.bookapp.service.CategoryService;
import org.springframework.http.ResponseEntity;
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
    public List<Category> getAll() {
        return categoryService.findAll();
    }

    @PostMapping
    public Category create(@RequestBody CategoryService.CategoryRequest req) {
        return categoryService.create(req);
    }

    @PutMapping("/{id}")
    public Category update(@PathVariable Long id,
                            @RequestBody CategoryService.CategoryRequest req) {
        return categoryService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            categoryService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
