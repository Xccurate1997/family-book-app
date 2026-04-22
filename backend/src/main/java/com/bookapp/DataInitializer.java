package com.bookapp;

import com.bookapp.entity.EffectRule;
import com.bookapp.entity.EffectType;
import com.bookapp.entity.EmoticonRule;
import com.bookapp.entity.SplashType;
import com.bookapp.entity.Theme;
import com.bookapp.entity.TransactionType;
import com.bookapp.entity.User;
import com.bookapp.entity.UserRole;
import com.bookapp.repository.EffectRuleRepository;
import com.bookapp.repository.EmoticonRuleRepository;
import com.bookapp.repository.ThemeRepository;
import com.bookapp.repository.UserRepository;
import com.bookapp.service.EffectRuleService;
import com.bookapp.service.EmoticonService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EmoticonRuleRepository emoticonRuleRepository;
    private final EffectRuleRepository effectRuleRepository;
    private final ThemeRepository themeRepository;
    private final EmoticonService emoticonService;
    private final EffectRuleService effectRuleService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           EmoticonRuleRepository emoticonRuleRepository,
                           EffectRuleRepository effectRuleRepository,
                           ThemeRepository themeRepository,
                           EmoticonService emoticonService,
                           EffectRuleService effectRuleService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emoticonRuleRepository = emoticonRuleRepository;
        this.effectRuleRepository = effectRuleRepository;
        this.themeRepository = themeRepository;
        this.emoticonService = emoticonService;
        this.effectRuleService = effectRuleService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Step 1: 初始化默认管理员账号
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("管理员");
            admin.setRole(UserRole.ADMIN);
            userRepository.save(admin);
        }

        // Step 2: 初始化默认情绪表情规则（系统级）
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

        // Step 3: 初始化默认彩蛋规则（系统级）
        if (effectRuleRepository.count() == 0) {
            List<EffectRule> effectRules = List.of(
                makeEffectRule("520 爱心", EffectType.HEARTS, "520", null, null, 100),
                makeEffectRule("1314 烟花", EffectType.FIREWORKS, "1314", null, null, 90),
                makeEffectRule("万元金币雨", EffectType.GOLD_RAIN, null, "10000", null, 50)
            );
            effectRuleRepository.saveAll(effectRules);
        }

        // Step 4: 初始化默认主题
        if (themeRepository.count() == 0) {
            Theme defaultTheme = new Theme();
            defaultTheme.setName("默认主题");
            defaultTheme.setDescription("简洁清爽的默认主题");
            defaultTheme.setDefault(true);
            defaultTheme.setPrimaryColor("#409eff");
            defaultTheme.setHeaderBg("#ffffff");
            defaultTheme.setSplashType(SplashType.NONE);
            defaultTheme.setSplashDuration(2000);
            themeRepository.save(defaultTheme);
        }

        // 确保缓存包含最新规则
        emoticonService.refreshCache();
        effectRuleService.refreshCache();
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

    private EffectRule makeEffectRule(String name, EffectType effectType,
                                      String exact, String min, String max, int priority) {
        EffectRule rule = new EffectRule();
        rule.setName(name);
        rule.setEffectType(effectType);
        rule.setExactAmount(exact != null ? new java.math.BigDecimal(exact) : null);
        rule.setMinAmount(min != null ? new java.math.BigDecimal(min) : null);
        rule.setMaxAmount(max != null ? new java.math.BigDecimal(max) : null);
        rule.setPriority(priority);
        rule.setEnabled(true);
        return rule;
    }
}
