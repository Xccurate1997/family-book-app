package com.bookapp.repository;

import com.bookapp.entity.EffectEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EffectEventRepository extends JpaRepository<EffectEvent, Long> {

    Optional<EffectEvent> findFirstByVideoUrlIsNotNullOrderByCreatedAtDesc();
}
