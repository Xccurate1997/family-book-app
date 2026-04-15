package com.bookapp;

import com.bookapp.entity.Category;
import com.bookapp.entity.TransactionType;
import com.bookapp.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public DataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return;
        }
        List<Category> defaults = List.of(
            new Category(null, "餐饮", TransactionType.EXPENSE, "🍜"),
            new Category(null, "购物", TransactionType.EXPENSE, "🛍️"),
            new Category(null, "交通", TransactionType.EXPENSE, "🚌"),
            new Category(null, "住房", TransactionType.EXPENSE, "🏠"),
            new Category(null, "娱乐", TransactionType.EXPENSE, "🎮"),
            new Category(null, "医疗", TransactionType.EXPENSE, "💊"),
            new Category(null, "教育", TransactionType.EXPENSE, "📚"),
            new Category(null, "其他支出", TransactionType.EXPENSE, "📦"),
            new Category(null, "工资", TransactionType.INCOME, "💰"),
            new Category(null, "奖金", TransactionType.INCOME, "🎁"),
            new Category(null, "投资", TransactionType.INCOME, "📈"),
            new Category(null, "其他收入", TransactionType.INCOME, "💵")
        );
        categoryRepository.saveAll(defaults);
    }
}
