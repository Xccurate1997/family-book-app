package com.bookapp.repository;

import com.bookapp.entity.EmoticonRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmoticonRuleRepository extends JpaRepository<EmoticonRule, Long> {

    List<EmoticonRule> findByEnabledTrueOrderByPriorityDesc();
}
