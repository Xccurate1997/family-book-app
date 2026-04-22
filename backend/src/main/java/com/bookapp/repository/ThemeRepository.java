package com.bookapp.repository;

import com.bookapp.entity.Theme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ThemeRepository extends JpaRepository<Theme, Long> {

    Optional<Theme> findByIsDefaultTrue();

    List<Theme> findByIdIn(List<Long> ids);
}
