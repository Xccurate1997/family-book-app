package com.bookapp.service;

import com.bookapp.entity.EffectEvent;
import com.bookapp.repository.EffectEventRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class EffectService {

    private static final Path VIDEO_DIR = Paths.get(
            System.getProperty("user.home"), ".bookapp", "videos");
    private static final long MAX_VIDEO_SIZE = 20 * 1024 * 1024;

    private final EffectEventRepository effectEventRepository;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public EffectService(EffectEventRepository effectEventRepository) {
        this.effectEventRepository = effectEventRepository;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(VIDEO_DIR);
    }

    /**
     * 创建 SSE 连接，注册 emitter
     */
    public SseEmitter createSseConnection() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(throwable -> emitters.remove(emitter));

        return emitter;
    }

    /**
     * 向所有 SSE 客户端推送特效事件，携带 effectType 和可选的 videoUrl
     */
    public void pushEffect(EffectEvent event) {
        EffectEvent saved = effectEventRepository.save(event);

        HashMap<String, Object> data = new HashMap<>();
        data.put("id", saved.getId());
        data.put("type", saved.getType());
        data.put("videoUrl", saved.getVideoUrl());

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("effect")
                        .data(data));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    /**
     * 标记特效已播放
     */
    public void markPlayed(Long id) {
        effectEventRepository.findById(id).ifPresent(event -> {
            event.setPlayed(true);
            effectEventRepository.save(event);
        });
    }

    // ── 视频文件管理 ────────────────────────────────────

    /**
     * 保存上传的视频文件，返回文件名
     */
    public String saveVideo(MultipartFile file) throws IOException {
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new IllegalArgumentException("视频文件不能超过 20MB");
        }
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString().substring(0, 8) + ext;
        Files.copy(file.getInputStream(), VIDEO_DIR.resolve(filename));
        return filename;
    }

    /**
     * 获取视频文件路径（带路径遍历防护）
     */
    public Path getVideoPath(String filename) {
        Path resolved = VIDEO_DIR.resolve(filename).normalize();
        if (!resolved.startsWith(VIDEO_DIR)) {
            throw new IllegalArgumentException("非法文件名");
        }
        return resolved;
    }
}
