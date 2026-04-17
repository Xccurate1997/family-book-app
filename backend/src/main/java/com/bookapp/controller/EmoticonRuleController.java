package com.bookapp.controller;

import com.bookapp.entity.EmoticonRule;
import com.bookapp.service.EmoticonService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EmoticonRuleController {

    private final EmoticonService emoticonService;

    public EmoticonRuleController(EmoticonService emoticonService) {
        this.emoticonService = emoticonService;
    }

    @GetMapping("/emoticon-rules")
    public List<EmoticonRule> getAll() {
        return emoticonService.getAllRules();
    }

    @PostMapping("/emoticon-rules")
    public EmoticonRule create(@RequestBody EmoticonRule rule) {
        return emoticonService.createRule(rule);
    }

    @PutMapping("/emoticon-rules/{id}")
    public EmoticonRule update(@PathVariable Long id, @RequestBody EmoticonRule rule) {
        return emoticonService.updateRule(id, rule);
    }

    @DeleteMapping("/emoticon-rules/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        emoticonService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/emoticon-rules/upload-image")
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        String filename = emoticonService.saveImage(file);
        return Map.of("filename", filename);
    }

    @GetMapping("/emoticon-images/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) throws IOException {
        Path path = emoticonService.getImagePath(filename);
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
