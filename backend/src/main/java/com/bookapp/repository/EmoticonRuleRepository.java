package com.bookapp.repository;

import java.util.List;

import com.bookapp.entity.EmoticonRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface EmoticonRuleRepository extends JpaRepository<EmoticonRule, Long> {

    List<EmoticonRule> findByEnabledTrueOrderByPriorityDesc();

    @Modifying
    @Transactional
    @Query("UPDATE EmoticonRule e SET e.enabled = false")
    int disableAllRule();

    @Modifying
    @Transactional
    @Query("UPDATE EmoticonRule e SET e.enabled = true")
    int enableAllRule();
}
