package com.bookapp.service;

import com.bookapp.entity.EffectRule;
import com.bookapp.entity.Transaction;
import com.bookapp.repository.EffectRuleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class EffectRuleService {

    private final EffectRuleRepository ruleRepo;
    private volatile List<EffectRule> cachedRules;

    public EffectRuleService(EffectRuleRepository ruleRepo) {
        this.ruleRepo = ruleRepo;
    }

    @PostConstruct
    public void init() {
        refreshCache();
    }

    public void refreshCache() {
        cachedRules = ruleRepo.findByEnabledTrueOrderByPriorityDesc();
    }

    /**
     * 评估交易是否匹配某条彩蛋规则，返回第一个匹配的规则，无匹配返回 null
     */
    public EffectRule evaluate(Transaction transaction) {
        BigDecimal amount = transaction.getAmount();
        Long categoryId = transaction.getCategory() != null ? transaction.getCategory().getId() : null;

        for (EffectRule rule : cachedRules) {
            if (!matches(rule, transaction, amount, categoryId)) {
                continue;
            }
            return rule;
        }
        return null;
    }

    private boolean matches(EffectRule rule, Transaction transaction, BigDecimal amount, Long categoryId) {
        // 交易类型匹配
        if (rule.getTransactionType() != null && rule.getTransactionType() != transaction.getType()) {
            return false;
        }
        // 分类匹配
        if (rule.getCategoryId() != null && !rule.getCategoryId().equals(categoryId)) {
            return false;
        }
        // 精确金额匹配（优先于范围）
        if (rule.getExactAmount() != null) {
            return amount.compareTo(rule.getExactAmount()) == 0;
        }
        // 金额下限（含）
        if (rule.getMinAmount() != null && amount.compareTo(rule.getMinAmount()) < 0) {
            return false;
        }
        // 金额上限（不含）
        if (rule.getMaxAmount() != null && amount.compareTo(rule.getMaxAmount()) >= 0) {
            return false;
        }
        return true;
    }

    // ── CRUD ──────────────────────────────────────────

    public List<EffectRule> getAllRules() {
        return ruleRepo.findAll();
    }

    public EffectRule createRule(EffectRule rule) {
        EffectRule saved = ruleRepo.save(rule);
        refreshCache();
        return saved;
    }

    public EffectRule updateRule(Long id, EffectRule updated) {
        EffectRule rule = ruleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("彩蛋规则不存在: " + id));
        rule.setName(updated.getName());
        rule.setEffectType(updated.getEffectType());
        rule.setVideoFilename(updated.getVideoFilename());
        rule.setTransactionType(updated.getTransactionType());
        rule.setExactAmount(updated.getExactAmount());
        rule.setMinAmount(updated.getMinAmount());
        rule.setMaxAmount(updated.getMaxAmount());
        rule.setCategoryId(updated.getCategoryId());
        rule.setPriority(updated.getPriority());
        rule.setEnabled(updated.isEnabled());
        EffectRule saved = ruleRepo.save(rule);
        refreshCache();
        return saved;
    }

    public void deleteRule(Long id) {
        ruleRepo.deleteById(id);
        refreshCache();
    }
}
