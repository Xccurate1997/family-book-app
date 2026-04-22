package com.bookapp.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bookapp.entity.EmoticonRule;
import com.bookapp.entity.Transaction;
import com.bookapp.repository.EmoticonRuleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EmoticonService {

    private static final Path EMOJI_DIR = Paths.get(
            System.getProperty("user.home"), ".bookapp", "emojis");

    private final EmoticonRuleRepository ruleRepo;
    private volatile List<EmoticonRule> cachedRules;

    public EmoticonService(EmoticonRuleRepository ruleRepo) {
        this.ruleRepo = ruleRepo;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(EMOJI_DIR);
        refreshCache();
    }

    public void refreshCache() {
        cachedRules = ruleRepo.findByEnabledTrueOrderByPriorityDesc();
    }

    /**
     * 评估交易，返回匹配的情绪表情，无匹配返回 null
     */
    public Map<String, String> evaluate(Transaction tx) {
        BigDecimal amount = tx.getAmount();
        Long catId = tx.getCategory() != null ? tx.getCategory().getId() : null;

        for (EmoticonRule rule : cachedRules) {
            // 类型匹配
            if (rule.getTransactionType() != null && rule.getTransactionType() != tx.getType()) {
                continue;
            }
            // 分类匹配
            if (rule.getCategoryId() != null && !rule.getCategoryId().equals(catId)) {
                continue;
            }
            // 金额下限
            if (rule.getMinAmount() != null && amount.compareTo(rule.getMinAmount()) < 0) {
                continue;
            }
            // 金额上限（不含）
            if (rule.getMaxAmount() != null && amount.compareTo(rule.getMaxAmount()) >= 0) {
                continue;
            }
            // 匹配成功
            return Map.of("type", rule.getEmojiType(), "content", rule.getEmojiContent());
        }
        return null;
    }

    /**
     * 批量填充交易列表的 moodEmoji
     */
    public void fillMoodEmoji(List<Transaction> transactions) {
        for (Transaction tx : transactions) {
            tx.setMoodEmoji(evaluate(tx));
        }
    }

    // ── CRUD ──────────────────────────────────────────

    public int disableAllRule() {
        int count = ruleRepo.disableAllRule();
        refreshCache();
        return count;
    }

    public int enableAllRule() {
        int count = ruleRepo.enableAllRule();
        refreshCache();
        return count;
    }

    public List<EmoticonRule> getAllRules() {
        return ruleRepo.findAll();
    }

    public EmoticonRule createRule(EmoticonRule rule) {
        EmoticonRule saved = ruleRepo.save(rule);
        refreshCache();
        return saved;
    }

    public EmoticonRule updateRule(Long id, EmoticonRule updated) {
        EmoticonRule rule = ruleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + id));
        rule.setName(updated.getName());
        rule.setEmojiType(updated.getEmojiType());
        rule.setEmojiContent(updated.getEmojiContent());
        rule.setTransactionType(updated.getTransactionType());
        rule.setMinAmount(updated.getMinAmount());
        rule.setMaxAmount(updated.getMaxAmount());
        rule.setCategoryId(updated.getCategoryId());
        rule.setPriority(updated.getPriority());
        rule.setEnabled(updated.isEnabled());
        EmoticonRule saved = ruleRepo.save(rule);
        refreshCache();
        return saved;
    }

    public void deleteRule(Long id) {
        ruleRepo.deleteById(id);
        refreshCache();
    }

    // ── 图片管理 ────────────────────────────────────────

    public String saveImage(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString().substring(0, 8) + ext;
        Files.copy(file.getInputStream(), EMOJI_DIR.resolve(filename));
        return filename;
    }

    public Path getImagePath(String filename) {
        // 防止路径遍历攻击
        Path resolved = EMOJI_DIR.resolve(filename).normalize();
        if (!resolved.startsWith(EMOJI_DIR)) {
            throw new IllegalArgumentException("非法文件名");
        }
        return resolved;
    }
}
