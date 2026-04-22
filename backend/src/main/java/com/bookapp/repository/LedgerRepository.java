package com.bookapp.repository;

import com.bookapp.entity.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {

    List<Ledger> findByUserId(Long userId);

    long countByUserId(Long userId);
}
