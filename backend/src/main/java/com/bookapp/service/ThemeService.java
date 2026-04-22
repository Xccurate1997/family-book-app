package com.bookapp.service;

import com.bookapp.entity.Theme;
import com.bookapp.entity.ThemeAssignment;
import com.bookapp.entity.ThemeDecoration;
import com.bookapp.entity.User;
import com.bookapp.repository.ThemeAssignmentRepository;
import com.bookapp.repository.ThemeDecorationRepository;
import com.bookapp.repository.ThemeRepository;
import com.bookapp.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ThemeService {

    private static final Path ASSETS_DIR = Paths.get(
            System.getProperty("user.home"), ".bookapp", "theme-assets");

    // 主题资源文件大小限制：图片最大 5MB，音效最大 10MB
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_SOUND_SIZE = 10 * 1024 * 1024;

    private final ThemeRepository themeRepository;
    private final ThemeDecorationRepository decorationRepository;
    private final ThemeAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    public ThemeService(ThemeRepository themeRepository,
                        ThemeDecorationRepository decorationRepository,
                        ThemeAssignmentRepository assignmentRepository,
                        UserRepository userRepository) {
        this.themeRepository = themeRepository;
        this.decorationRepository = decorationRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(ASSETS_DIR);
    }

    // ── 主题 CRUD ──────────────────────────────────────

    public List<Theme> getAllThemes() {
        return themeRepository.findAll();
    }

    public Theme getThemeById(Long id) {
        return themeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("主题不存在: " + id));
    }

    @Transactional
    public Theme createTheme(Theme theme) {
        if (theme.isDefault()) {
            clearDefaultFlag();
        }
        List<ThemeDecoration> decorations = theme.getDecorations();
        theme.setDecorations(new ArrayList<>());
        Theme saved = themeRepository.save(theme);

        if (decorations != null) {
            for (ThemeDecoration decoration : decorations) {
                decoration.setTheme(saved);
            }
            decorationRepository.saveAll(decorations);
            saved.setDecorations(decorations);
        }
        return saved;
    }

    @Transactional
    public Theme updateTheme(Long id, Theme updated) {
        Theme theme = getThemeById(id);

        if (updated.isDefault() && !theme.isDefault()) {
            clearDefaultFlag();
        }

        theme.setName(updated.getName());
        theme.setDescription(updated.getDescription());
        theme.setDefault(updated.isDefault());
        theme.setPrimaryColor(updated.getPrimaryColor());
        theme.setHeaderBg(updated.getHeaderBg());
        theme.setSplashType(updated.getSplashType());
        theme.setSplashImageFilename(updated.getSplashImageFilename());
        theme.setSplashDuration(updated.getSplashDuration());
        theme.setSoundFilename(updated.getSoundFilename());
        theme.setLogoFilename(updated.getLogoFilename());
        theme.setHeaderPatternFilename(updated.getHeaderPatternFilename());
        theme.setHeaderPatternOpacity(updated.getHeaderPatternOpacity());
        theme.setCornerDecoFilename(updated.getCornerDecoFilename());
        theme.setEmptyStateFilename(updated.getEmptyStateFilename());
        theme.setDecoOpacity(updated.getDecoOpacity());
        theme.setDecoMaxHeight(updated.getDecoMaxHeight());
        theme.setDecoMaxWidth(updated.getDecoMaxWidth());
        theme.setDecoFadeEdge(updated.getDecoFadeEdge());
        theme.setDecoFadeSide(updated.getDecoFadeSide());
        theme.setIncomeBgFilename(updated.getIncomeBgFilename());
        theme.setExpenseBgFilename(updated.getExpenseBgFilename());

        // 更新 decorations：先清空再重建
        theme.getDecorations().clear();
        if (updated.getDecorations() != null) {
            for (ThemeDecoration decoration : updated.getDecorations()) {
                decoration.setTheme(theme);
                theme.getDecorations().add(decoration);
            }
        }

        return themeRepository.save(theme);
    }

    @Transactional
    public void deleteTheme(Long id) {
        Theme theme = getThemeById(id);
        if (theme.isDefault()) {
            throw new IllegalStateException("不能删除默认主题");
        }
        themeRepository.delete(theme);
    }

    private void clearDefaultFlag() {
        themeRepository.findByIsDefaultTrue().ifPresent(existing -> {
            existing.setDefault(false);
            themeRepository.save(existing);
        });
    }

    // ── 用户可用主题 ───────────────────────────────────

    public List<Theme> getAvailableThemes(Long userId) {
        List<ThemeAssignment> assignments = assignmentRepository.findByUserId(userId);
        List<Long> assignedThemeIds = assignments.stream()
                .map(ThemeAssignment::getThemeId)
                .toList();

        List<Theme> result = new ArrayList<>();

        // 默认主题始终可用
        themeRepository.findByIsDefaultTrue().ifPresent(result::add);

        // 管理员分配的主题
        if (!assignedThemeIds.isEmpty()) {
            List<Theme> assignedThemes = themeRepository.findByIdIn(assignedThemeIds);
            for (Theme theme : assignedThemes) {
                if (!theme.isDefault()) {
                    result.add(theme);
                }
            }
        }

        return result;
    }

    // ── 主题分配 ───────────────────────────────────────

    /**
     * 获取主题的分配列表，返回包含用户名的信息
     */
    public List<java.util.Map<String, Object>> getAssignmentsWithUsername(Long themeId) {
        List<ThemeAssignment> assignments = assignmentRepository.findByThemeId(themeId);
        List<java.util.Map<String, Object>> result = new ArrayList<>();
        for (ThemeAssignment assignment : assignments) {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", assignment.getId());
            item.put("themeId", assignment.getThemeId());
            item.put("userId", assignment.getUserId());
            userRepository.findById(assignment.getUserId()).ifPresent(user -> {
                item.put("username", user.getUsername());
                item.put("nickname", user.getNickname());
            });
            result.add(item);
        }
        return result;
    }

    /**
     * 按用户名分配主题
     */
    public ThemeAssignment assignToUserByUsername(Long themeId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户名 \"" + username + "\" 不存在"));
        if (assignmentRepository.existsByThemeIdAndUserId(themeId, user.getId())) {
            throw new IllegalStateException("该用户已分配此主题");
        }
        ThemeAssignment assignment = new ThemeAssignment();
        assignment.setThemeId(themeId);
        assignment.setUserId(user.getId());
        return assignmentRepository.save(assignment);
    }

    /**
     * 按用户名移除主题分配
     */
    @Transactional
    public void removeAssignmentByUsername(Long themeId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户名 \"" + username + "\" 不存在"));
        assignmentRepository.deleteByThemeIdAndUserId(themeId, user.getId());
    }

    // ── 资源文件管理 ───────────────────────────────────

    /**
     * 保存主题图片资源（装饰图片、开屏图片），限制最大 5MB
     */
    public String saveImageAsset(MultipartFile file) throws IOException {
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("图片文件不能超过 5MB");
        }
        return saveFile(file);
    }

    /**
     * 保存主题音效资源，限制最大 10MB
     */
    public String saveSoundAsset(MultipartFile file) throws IOException {
        if (file.getSize() > MAX_SOUND_SIZE) {
            throw new IllegalArgumentException("音效文件不能超过 10MB");
        }
        return saveFile(file);
    }

    private String saveFile(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString().substring(0, 8) + ext;
        Files.copy(file.getInputStream(), ASSETS_DIR.resolve(filename));
        return filename;
    }

    public Path getAssetPath(String filename) {
        Path resolved = ASSETS_DIR.resolve(filename).normalize();
        if (!resolved.startsWith(ASSETS_DIR)) {
            throw new IllegalArgumentException("非法文件名");
        }
        return resolved;
    }
}
