package com.bookapp.controller;

import com.bookapp.entity.EffectRule;
import com.bookapp.service.EffectRuleService;
import com.bookapp.service.EffectService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UnbelievableController {

    private final EffectService effectService;
    private final EffectRuleService effectRuleService;

    public UnbelievableController(EffectService effectService, EffectRuleService effectRuleService) {
        this.effectService = effectService;
        this.effectRuleService = effectRuleService;
    }

    // ── SSE + 播放回调 ──────────────────────────────────

    @GetMapping(value = "/effects/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return effectService.createSseConnection();
    }

    @PostMapping("/effects/{id}/played")
    public ResponseEntity<Void> markPlayed(@PathVariable Long id) {
        effectService.markPlayed(id);
        return ResponseEntity.ok().build();
    }

    // ── 彩蛋规则 CRUD ───────────────────────────────────

    @GetMapping("/effect-rules")
    public List<EffectRule> getAllRules() {
        return effectRuleService.getAllRules();
    }

    @PostMapping("/effect-rules")
    public EffectRule createRule(@RequestBody EffectRule rule) {
        return effectRuleService.createRule(rule);
    }

    @PutMapping("/effect-rules/{id}")
    public EffectRule updateRule(@PathVariable Long id, @RequestBody EffectRule rule) {
        return effectRuleService.updateRule(id, rule);
    }

    @DeleteMapping("/effect-rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        effectRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    // ── 视频上传/获取 ───────────────────────────────────

    @PostMapping("/effect-videos/upload")
    public Map<String, String> uploadVideo(@RequestParam("file") MultipartFile file) throws IOException {
        String filename = effectService.saveVideo(file);
        return Map.of("filename", filename);
    }

    @GetMapping("/effect-videos/{filename}")
    public ResponseEntity<Resource> getVideo(@PathVariable String filename) throws IOException {
        Path path = effectService.getVideoPath(filename);
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
