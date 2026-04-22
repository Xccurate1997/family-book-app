package com.bookapp.repository;

import com.bookapp.entity.EffectRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EffectRuleRepository extends JpaRepository<EffectRule, Long> {

    List<EffectRule> findByEnabledTrueOrderByPriorityDesc();
}
