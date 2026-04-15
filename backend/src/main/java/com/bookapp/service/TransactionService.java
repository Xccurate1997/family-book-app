package com.bookapp.service;

import com.bookapp.entity.Category;
import com.bookapp.entity.Transaction;
import com.bookapp.entity.TransactionType;
import com.bookapp.repository.CategoryRepository;
import com.bookapp.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepo;
    private final CategoryRepository categoryRepo;

    public TransactionService(TransactionRepository transactionRepo, CategoryRepository categoryRepo) {
        this.transactionRepo = transactionRepo;
        this.categoryRepo = categoryRepo;
    }

    public List<Transaction> findByMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return transactionRepo.findByTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                ym.atDay(1), ym.atEndOfMonth());
    }

    public Map<String, BigDecimal> getSummary(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        BigDecimal income = Optional.ofNullable(
                transactionRepo.sumByTypeAndDateBetween(TransactionType.INCOME, start, end)
        ).orElse(BigDecimal.ZERO);

        BigDecimal expense = Optional.ofNullable(
                transactionRepo.sumByTypeAndDateBetween(TransactionType.EXPENSE, start, end)
        ).orElse(BigDecimal.ZERO);

        return Map.of(
                "totalIncome", income,
                "totalExpense", expense,
                "balance", income.subtract(expense)
        );
    }

    public Transaction create(TransactionRequest req) {
        Category category = categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + req.categoryId()));
        Transaction tx = new Transaction();
        tx.setAmount(req.amount());
        tx.setType(TransactionType.valueOf(req.type()));
        tx.setCategory(category);
        tx.setDescription(req.description());
        tx.setTransactionDate(req.transactionDate());
        return transactionRepo.save(tx);
    }

    public Transaction update(Long id, TransactionRequest req) {
        Transaction tx = transactionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
        Category category = categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + req.categoryId()));
        tx.setAmount(req.amount());
        tx.setType(TransactionType.valueOf(req.type()));
        tx.setCategory(category);
        tx.setDescription(req.description());
        tx.setTransactionDate(req.transactionDate());
        return transactionRepo.save(tx);
    }

    public void delete(Long id) {
        transactionRepo.deleteById(id);
    }

    public record TransactionRequest(
            BigDecimal amount,
            String type,
            Long categoryId,
            String description,
            LocalDate transactionDate) {
    }
}
