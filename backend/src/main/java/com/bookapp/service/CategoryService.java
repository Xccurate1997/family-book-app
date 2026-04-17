package com.bookapp.service;

import com.bookapp.entity.Category;
import com.bookapp.entity.TransactionType;
import com.bookapp.repository.CategoryRepository;
import com.bookapp.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category create(CategoryRequest req) {
        Category category = new Category();
        category.setName(req.name());
        category.setType(TransactionType.valueOf(req.type()));
        category.setIcon(req.icon());
        return categoryRepository.save(category);
    }

    public Category update(Long id, CategoryRequest req) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在: " + id));
        category.setName(req.name());
        category.setIcon(req.icon());
        return categoryRepository.save(category);
    }

    public void delete(Long id) {
        if (transactionRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("该分类下存在交易记录，无法删除");
        }
        categoryRepository.deleteById(id);
    }

    public record CategoryRequest(String name, String type, String icon) {}
}
