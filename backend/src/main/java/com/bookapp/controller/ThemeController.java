package com.bookapp.controller;

import com.bookapp.entity.Theme;
import com.bookapp.entity.ThemeAssignment;
import com.bookapp.service.ThemeService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ThemeController {

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    // ── 管理员：主题 CRUD ────────────────────────────────

    @GetMapping("/themes")
    public List<Theme> getAllThemes() {
        return themeService.getAllThemes();
    }

    @GetMapping("/themes/{id}")
    public Theme getTheme(@PathVariable Long id) {
        return themeService.getThemeById(id);
    }

    @PostMapping("/themes")
    public Theme createTheme(@RequestBody Theme theme) {
        return themeService.createTheme(theme);
    }

    @PutMapping("/themes/{id}")
    public Theme updateTheme(@PathVariable Long id, @RequestBody Theme theme) {
        return themeService.updateTheme(id, theme);
    }

    @DeleteMapping("/themes/{id}")
    public ResponseEntity<Void> deleteTheme(@PathVariable Long id) {
        themeService.deleteTheme(id);
        return ResponseEntity.noContent().build();
    }

    // ── 管理员：主题分配 ─────────────────────────────────

    @GetMapping("/themes/{id}/assignments")
    public List<Map<String, Object>> getAssignments(@PathVariable Long id) {
        return themeService.getAssignmentsWithUsername(id);
    }

    @PostMapping("/themes/{id}/assignments")
    public ThemeAssignment assignUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String username = body.get("username");
        return themeService.assignToUserByUsername(id, username);
    }

    @DeleteMapping("/themes/{themeId}/assignments/{username}")
    public ResponseEntity<Void> removeAssignment(@PathVariable Long themeId, @PathVariable String username) {
        themeService.removeAssignmentByUsername(themeId, username);
        return ResponseEntity.noContent().build();
    }

    // ── 管理员：资源上传 ─────────────────────────────────

    @PostMapping("/themes/upload-image")
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        // 图片限制 5MB，见 ThemeService.MAX_IMAGE_SIZE
        String filename = themeService.saveImageAsset(file);
        return Map.of("filename", filename);
    }

    @PostMapping("/themes/upload-sound")
    public Map<String, String> uploadSound(@RequestParam("file") MultipartFile file) throws IOException {
        // 音效限制 10MB，见 ThemeService.MAX_SOUND_SIZE
        String filename = themeService.saveSoundAsset(file);
        return Map.of("filename", filename);
    }

    // ── 用户：可用主题 ───────────────────────────────────

    @GetMapping("/themes/available")
    public List<Theme> getAvailableThemes(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return themeService.getAvailableThemes(userId);
    }

    // ── 静态资源 ─────────────────────────────────────────

    @GetMapping("/theme-assets/{filename}")
    public ResponseEntity<Resource> getAsset(@PathVariable String filename) throws IOException {
        Path path = themeService.getAssetPath(filename);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        String contentType = Files.probeContentType(path);
        if (contentType == null) contentType = "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(new FileSystemResource(path));
    }
}
