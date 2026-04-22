package com.bookapp.repository;

import com.bookapp.entity.ThemeDecoration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThemeDecorationRepository extends JpaRepository<ThemeDecoration, Long> {

    List<ThemeDecoration> findByThemeId(Long themeId);

    void deleteByThemeId(Long themeId);
}
