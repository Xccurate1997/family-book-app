package com.bookapp;

import com.bookapp.entity.Category;
import com.bookapp.entity.Ledger;
import com.bookapp.entity.TransactionType;
import com.bookapp.repository.CategoryRepository;
import com.bookapp.repository.LedgerRepository;
import com.bookapp.repository.TransactionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final LedgerRepository ledgerRepository;
    private final TransactionRepository transactionRepository;

    public DataInitializer(CategoryRepository categoryRepository,
                           LedgerRepository ledgerRepository,
                           TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.ledgerRepository = ledgerRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Step 1: 初始化默认账本
        Ledger defaultLedger;
        if (ledgerRepository.count() == 0) {
            defaultLedger = new Ledger();
            defaultLedger.setName("日常账本");
            defaultLedger.setIcon("📒");
            defaultLedger.setColor("#4A90D9");
            defaultLedger = ledgerRepository.save(defaultLedger);
        } else {
            defaultLedger = ledgerRepository.findAll().get(0);
        }

        // Step 2: 初始化默认分类
        if (categoryRepository.count() == 0) {
            List<Category> defaults = List.of(
                new Category(null, "餐饮", TransactionType.EXPENSE, "🍜"),
                new Category(null, "购物", TransactionType.EXPENSE, "🛍️"),
                new Category(null, "交通", TransactionType.EXPENSE, "🚌"),
                new Category(null, "住房", TransactionType.EXPENSE, "🏠"),
                new Category(null, "娱乐", TransactionType.EXPENSE, "🎮"),
                new Category(null, "医疗", TransactionType.EXPENSE, "💊"),
                new Category(null, "教育", TransactionType.EXPENSE, "📚"),
                new Category(null, "其他支出", TransactionType.EXPENSE, "📦"),
                new Category(null, "工资", TransactionType.INCOME, "💰"),
                new Category(null, "奖金", TransactionType.INCOME, "🎁"),
                new Category(null, "投资", TransactionType.INCOME, "📈"),
                new Category(null, "其他收入", TransactionType.INCOME, "💵")
            );
            categoryRepository.saveAll(defaults);
        }

        // Step 3: 迁移旧数据——将 ledger_id 为空的交易归入默认账本
        if (transactionRepository.countByLedgerIsNull() > 0) {
            transactionRepository.assignDefaultLedger(defaultLedger);
        }
    }
}
