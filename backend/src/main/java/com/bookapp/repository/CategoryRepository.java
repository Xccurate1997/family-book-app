package com.bookapp.repository;

import com.bookapp.entity.Category;
import com.bookapp.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserId(Long userId);

    List<Category> findByUserIdAndType(Long userId, TransactionType type);
}
