package com.bookapp;

import com.bookapp.entity.Category;
import com.bookapp.entity.EmoticonRule;
import com.bookapp.entity.Ledger;
import com.bookapp.entity.TransactionType;
import com.bookapp.repository.CategoryRepository;
import com.bookapp.repository.EmoticonRuleRepository;
import com.bookapp.repository.LedgerRepository;
import com.bookapp.repository.TransactionRepository;
import com.bookapp.service.EmoticonService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final LedgerRepository ledgerRepository;
    private final TransactionRepository transactionRepository;
    private final EmoticonRuleRepository emoticonRuleRepository;
    private final EmoticonService emoticonService;

    public DataInitializer(CategoryRepository categoryRepository,
                           LedgerRepository ledgerRepository,
                           TransactionRepository transactionRepository,
                           EmoticonRuleRepository emoticonRuleRepository,
                           EmoticonService emoticonService) {
        this.categoryRepository = categoryRepository;
        this.ledgerRepository = ledgerRepository;
        this.transactionRepository = transactionRepository;
        this.emoticonRuleRepository = emoticonRuleRepository;
        this.emoticonService = emoticonService;
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

        // Step 4: 初始化默认情绪表情规则
        if (emoticonRuleRepository.count() == 0) {
            List<EmoticonRule> rules = List.of(
                makeRule("小额支出", "TEXT", "😊", TransactionType.EXPENSE, "0", "50", 10),
                makeRule("中等支出", "TEXT", "😐", TransactionType.EXPENSE, "50", "200", 20),
                makeRule("较大支出", "TEXT", "😰", TransactionType.EXPENSE, "200", "500", 30),
                makeRule("大额支出", "TEXT", "😱", TransactionType.EXPENSE, "500", null, 40),
                makeRule("收入开心", "TEXT", "😄", TransactionType.INCOME, "0", "5000", 50),
                makeRule("大额收入", "TEXT", "🎉", TransactionType.INCOME, "5000", null, 60)
            );
            emoticonRuleRepository.saveAll(rules);
        }
        // 确保缓存包含最新规则（@PostConstruct 在 CommandLineRunner 之前执行，缓存可能为空）
        emoticonService.refreshCache();
    }

    private EmoticonRule makeRule(String name, String emojiType, String emojiContent,
                                  TransactionType txType, String min, String max, int priority) {
        EmoticonRule rule = new EmoticonRule();
        rule.setName(name);
        rule.setEmojiType(emojiType);
        rule.setEmojiContent(emojiContent);
        rule.setTransactionType(txType);
        rule.setMinAmount(min != null ? new java.math.BigDecimal(min) : null);
        rule.setMaxAmount(max != null ? new java.math.BigDecimal(max) : null);
        rule.setPriority(priority);
        rule.setEnabled(true);
        return rule;
    }
}
