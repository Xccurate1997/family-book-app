package com.bookapp.repository;

import com.bookapp.entity.Transaction;
import com.bookapp.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            LocalDate start, LocalDate end);

    @Query("SELECT SUM(t.amount) FROM Transaction t " +
           "WHERE t.type = :type AND t.transactionDate BETWEEN :start AND :end")
    BigDecimal sumByTypeAndDateBetween(
            @Param("type") TransactionType type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
