package com.bookapp.repository;

import com.bookapp.entity.ThemeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThemeAssignmentRepository extends JpaRepository<ThemeAssignment, Long> {

    List<ThemeAssignment> findByUserId(Long userId);

    List<ThemeAssignment> findByThemeId(Long themeId);

    void deleteByThemeIdAndUserId(Long themeId, Long userId);

    boolean existsByThemeIdAndUserId(Long themeId, Long userId);
}
