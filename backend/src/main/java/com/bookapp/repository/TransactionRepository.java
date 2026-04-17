package com.bookapp.repository;

import com.bookapp.entity.Ledger;
import com.bookapp.entity.Transaction;
import com.bookapp.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByLedgerIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            Long ledgerId, LocalDate start, LocalDate end);

    @Query("SELECT SUM(t.amount) FROM Transaction t " +
           "WHERE t.ledger.id = :ledgerId AND t.type = :type " +
           "AND t.transactionDate BETWEEN :start AND :end")
    BigDecimal sumByLedgerAndTypeAndDateBetween(
            @Param("ledgerId") Long ledgerId,
            @Param("type") TransactionType type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // 统计：按分类汇总支出
    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t " +
           "WHERE t.ledger.id = :ledgerId AND t.type = 'EXPENSE' " +
           "AND t.transactionDate BETWEEN :start AND :end " +
           "GROUP BY t.category")
    List<Object[]> sumExpenseByCategoryAndDateBetween(
            @Param("ledgerId") Long ledgerId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // 统计：按日期和类型汇总
    @Query("SELECT t.transactionDate, t.type, SUM(t.amount) FROM Transaction t " +
           "WHERE t.ledger.id = :ledgerId " +
           "AND t.transactionDate BETWEEN :start AND :end " +
           "GROUP BY t.transactionDate, t.type " +
           "ORDER BY t.transactionDate")
    List<Object[]> sumByDateAndType(
            @Param("ledgerId") Long ledgerId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // 迁移辅助
    long countByLedgerIsNull();

    @Modifying
    @Query("UPDATE Transaction t SET t.ledger = :ledger WHERE t.ledger IS NULL")
    void assignDefaultLedger(@Param("ledger") Ledger ledger);

    // 按日期查询
    List<Transaction> findByLedgerIdAndTransactionDateOrderByCreatedAtDesc(
            Long ledgerId, LocalDate transactionDate);

    // 删除校验
    boolean existsByCategoryId(Long categoryId);

    boolean existsByLedgerId(Long ledgerId);
}
