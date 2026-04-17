package com.bookapp.service;

import com.bookapp.entity.Category;
import com.bookapp.entity.TransactionType;
import com.bookapp.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StatsService {

    private final TransactionRepository transactionRepo;

    public StatsService(TransactionRepository transactionRepo) {
        this.transactionRepo = transactionRepo;
    }

    public List<MonthlyTrendItem> monthlyTrend(Long ledgerId, int months) {
        List<MonthlyTrendItem> result = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();

            BigDecimal income = Optional.ofNullable(
                    transactionRepo.sumByLedgerAndTypeAndDateBetween(
                            ledgerId, TransactionType.INCOME, start, end))
                    .orElse(BigDecimal.ZERO);
            BigDecimal expense = Optional.ofNullable(
                    transactionRepo.sumByLedgerAndTypeAndDateBetween(
                            ledgerId, TransactionType.EXPENSE, start, end))
                    .orElse(BigDecimal.ZERO);

            result.add(new MonthlyTrendItem(ym.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    income, expense));
        }
        return result;
    }

    public List<CategoryBreakdownItem> categoryBreakdown(Long ledgerId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        List<Object[]> rows = transactionRepo.sumExpenseByCategoryAndDateBetween(
                ledgerId, ym.atDay(1), ym.atEndOfMonth());

        List<CategoryBreakdownItem> result = new ArrayList<>();
        for (Object[] row : rows) {
            Category cat = (Category) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            result.add(new CategoryBreakdownItem(cat.getId(), cat.getName(), cat.getIcon(), amount));
        }
        return result;
    }

    public List<DailyStatItem> dailyStats(Long ledgerId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        List<Object[]> rows = transactionRepo.sumByDateAndType(
                ledgerId, ym.atDay(1), ym.atEndOfMonth());

        Map<Integer, DailyStatItem> dayMap = new HashMap<>();
        for (Object[] row : rows) {
            LocalDate date = (LocalDate) row[0];
            TransactionType type = (TransactionType) row[1];
            BigDecimal amount = (BigDecimal) row[2];
            int day = date.getDayOfMonth();

            dayMap.putIfAbsent(day, new DailyStatItem(day, BigDecimal.ZERO, BigDecimal.ZERO));
            DailyStatItem item = dayMap.get(day);
            if (type == TransactionType.INCOME) {
                dayMap.put(day, new DailyStatItem(day, item.income().add(amount), item.expense()));
            } else {
                dayMap.put(day, new DailyStatItem(day, item.income(), item.expense().add(amount)));
            }
        }

        // 返回当月所有天（无数据的天 income/expense 为 0）
        List<DailyStatItem> result = new ArrayList<>();
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            result.add(dayMap.getOrDefault(d, new DailyStatItem(d, BigDecimal.ZERO, BigDecimal.ZERO)));
        }
        return result;
    }

    // ── 年度统计 ──────────────────────────────────────────

    public YearlySummaryItem yearlySummary(Long ledgerId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        BigDecimal income = Optional.ofNullable(
                transactionRepo.sumByLedgerAndTypeAndDateBetween(
                        ledgerId, TransactionType.INCOME, start, end))
                .orElse(BigDecimal.ZERO);
        BigDecimal expense = Optional.ofNullable(
                transactionRepo.sumByLedgerAndTypeAndDateBetween(
                        ledgerId, TransactionType.EXPENSE, start, end))
                .orElse(BigDecimal.ZERO);
        return new YearlySummaryItem(income, expense, income.subtract(expense));
    }

    public List<MonthlyTrendItem> yearlyMonthlyTrend(Long ledgerId, int year) {
        List<MonthlyTrendItem> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            BigDecimal income = Optional.ofNullable(
                    transactionRepo.sumByLedgerAndTypeAndDateBetween(
                            ledgerId, TransactionType.INCOME, start, end))
                    .orElse(BigDecimal.ZERO);
            BigDecimal expense = Optional.ofNullable(
                    transactionRepo.sumByLedgerAndTypeAndDateBetween(
                            ledgerId, TransactionType.EXPENSE, start, end))
                    .orElse(BigDecimal.ZERO);
            result.add(new MonthlyTrendItem(ym.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    income, expense));
        }
        return result;
    }

    public List<CategoryBreakdownItem> yearlyCategoryRanking(Long ledgerId, int year, int limit) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        List<Object[]> rows = transactionRepo.sumExpenseByCategoryAndDateBetween(
                ledgerId, start, end);

        List<CategoryBreakdownItem> result = new ArrayList<>();
        for (Object[] row : rows) {
            Category cat = (Category) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            result.add(new CategoryBreakdownItem(cat.getId(), cat.getName(), cat.getIcon(), amount));
        }
        // 按金额降序排列，取 top N
        result.sort((a, b) -> b.amount().compareTo(a.amount()));
        if (result.size() > limit) {
            result = result.subList(0, limit);
        }
        return result;
    }

    // ── Records ─────────────────────────────────────────

    public record MonthlyTrendItem(String month, BigDecimal income, BigDecimal expense) {}

    public record CategoryBreakdownItem(Long categoryId, String categoryName,
                                        String icon, BigDecimal amount) {}

    public record DailyStatItem(int day, BigDecimal income, BigDecimal expense) {}

    public record YearlySummaryItem(BigDecimal totalIncome, BigDecimal totalExpense,
                                    BigDecimal balance) {}
}
