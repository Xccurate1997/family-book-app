package com.bookapp.service;

import com.bookapp.config.JwtUtil;
import com.bookapp.entity.Category;
import com.bookapp.entity.Ledger;
import com.bookapp.entity.TransactionType;
import com.bookapp.entity.User;
import com.bookapp.entity.UserRole;
import com.bookapp.repository.CategoryRepository;
import com.bookapp.repository.LedgerRepository;
import com.bookapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private static final String INVITE_CODE = "pidan";

    private final UserRepository userRepository;
    private final LedgerRepository ledgerRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       LedgerRepository ledgerRepository,
                       CategoryRepository categoryRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.ledgerRepository = ledgerRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());

        return Map.of(
                "token", token,
                "userId", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname() != null ? user.getNickname() : user.getUsername(),
                "role", user.getRole().name()
        );
    }

    @Transactional
    public Map<String, Object> register(String username, String password, String nickname, String inviteCode) {
        if (!INVITE_CODE.equals(inviteCode)) {
            throw new RuntimeException("邀请码无效");
        }

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        if (username.length() < 2 || username.length() > 20) {
            throw new RuntimeException("用户名长度需在 2-20 个字符之间");
        }

        if (password.length() < 6) {
            throw new RuntimeException("密码长度不能少于 6 个字符");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null && !nickname.isBlank() ? nickname : username);
        user.setRole(UserRole.USER);
        user = userRepository.save(user);

        initializeUserData(user.getId());

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());

        return Map.of(
                "token", token,
                "userId", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "role", user.getRole().name()
        );
    }

    private void initializeUserData(Long userId) {
        Ledger ledger = new Ledger();
        ledger.setName("日常账本");
        ledger.setIcon("📒");
        ledger.setColor("#4A90D9");
        ledger.setUserId(userId);
        ledgerRepository.save(ledger);

        List<Category> categories = List.of(
                createCategory("餐饮", TransactionType.EXPENSE, "🍜", userId),
                createCategory("购物", TransactionType.EXPENSE, "🛍️", userId),
                createCategory("交通", TransactionType.EXPENSE, "🚌", userId),
                createCategory("住房", TransactionType.EXPENSE, "🏠", userId),
                createCategory("娱乐", TransactionType.EXPENSE, "🎮", userId),
                createCategory("医疗", TransactionType.EXPENSE, "💊", userId),
                createCategory("教育", TransactionType.EXPENSE, "📚", userId),
                createCategory("其他支出", TransactionType.EXPENSE, "📦", userId),
                createCategory("工资", TransactionType.INCOME, "💰", userId),
                createCategory("奖金", TransactionType.INCOME, "🎁", userId),
                createCategory("投资", TransactionType.INCOME, "📈", userId),
                createCategory("其他收入", TransactionType.INCOME, "💵", userId)
        );
        categoryRepository.saveAll(categories);
    }

    private Category createCategory(String name, TransactionType type, String icon, Long userId) {
        Category category = new Category();
        category.setName(name);
        category.setType(type);
        category.setIcon(icon);
        category.setUserId(userId);
        return category;
    }

    public Map<String, Object> getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname() != null ? user.getNickname() : user.getUsername(),
                "role", user.getRole().name()
        );
    }
}
