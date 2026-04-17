package com.bookapp.service;

import com.bookapp.entity.Category;
import com.bookapp.entity.Ledger;
import com.bookapp.entity.Transaction;
import com.bookapp.entity.TransactionType;
import com.bookapp.repository.CategoryRepository;
import com.bookapp.repository.LedgerRepository;
import com.bookapp.repository.TransactionRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepo;
    private final CategoryRepository categoryRepo;
    private final LedgerRepository ledgerRepo;

    public TransactionService(TransactionRepository transactionRepo,
                               CategoryRepository categoryRepo,
                               LedgerRepository ledgerRepo) {
        this.transactionRepo = transactionRepo;
        this.categoryRepo = categoryRepo;
        this.ledgerRepo = ledgerRepo;
    }

    public List<Transaction> findByMonth(Long ledgerId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return transactionRepo.findByLedgerIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                ledgerId, ym.atDay(1), ym.atEndOfMonth());
    }

    public List<Transaction> findFiltered(Long ledgerId, int year, int month,
                                           String keyword, Long categoryId,
                                           BigDecimal minAmount, BigDecimal maxAmount) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ledger").get("id"), ledgerId));
            predicates.add(cb.between(root.get("transactionDate"), start, end));
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("description")),
                        "%" + keyword.toLowerCase() + "%"));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepo.findAll(spec,
                Sort.by(Sort.Direction.DESC, "transactionDate", "createdAt"));
    }

    public Map<String, BigDecimal> getSummary(Long ledgerId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        BigDecimal income = Optional.ofNullable(
                transactionRepo.sumByLedgerAndTypeAndDateBetween(ledgerId, TransactionType.INCOME, start, end)
        ).orElse(BigDecimal.ZERO);

        BigDecimal expense = Optional.ofNullable(
                transactionRepo.sumByLedgerAndTypeAndDateBetween(ledgerId, TransactionType.EXPENSE, start, end)
        ).orElse(BigDecimal.ZERO);

        return Map.of("totalIncome", income, "totalExpense", expense,
                "balance", income.subtract(expense));
    }

    public Transaction create(TransactionRequest req) {
        Category category = categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("分类不存在: " + req.categoryId()));
        Ledger ledger = ledgerRepo.findById(req.ledgerId())
                .orElseThrow(() -> new IllegalArgumentException("账本不存在: " + req.ledgerId()));
        Transaction tx = new Transaction();
        tx.setAmount(req.amount());
        tx.setType(TransactionType.valueOf(req.type()));
        tx.setCategory(category);
        tx.setLedger(ledger);
        tx.setDescription(req.description());
        tx.setTransactionDate(req.transactionDate());
        return transactionRepo.save(tx);
    }

    public Transaction update(Long id, TransactionRequest req) {
        Transaction tx = transactionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("交易不存在: " + id));
        Category category = categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("分类不存在: " + req.categoryId()));
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

    public byte[] exportToCsv(Long ledgerId, int year, int month) {
        List<Transaction> list = findByMonth(ledgerId, year, month);
        StringBuilder sb = new StringBuilder();
        sb.append("日期,类型,分类,金额,备注\n");
        for (Transaction tx : list) {
            sb.append(tx.getTransactionDate()).append(",");
            sb.append(tx.getType() == TransactionType.INCOME ? "收入" : "支出").append(",");
            sb.append(tx.getCategory() != null ? tx.getCategory().getName() : "").append(",");
            sb.append(tx.getAmount()).append(",");
            sb.append(tx.getDescription() != null ? tx.getDescription() : "").append("\n");
        }
        // UTF-8 BOM for Excel compatibility
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);
        return result;
    }

    public record TransactionRequest(
            Long ledgerId,
            BigDecimal amount,
            String type,
            Long categoryId,
            String description,
            LocalDate transactionDate) {
    }
}
