package com.bookapp.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.bookapp.entity.EmoticonRule;
import com.bookapp.service.EmoticonService;
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

    @PostMapping("/emoticon-rules/enable-all")
    public Map<String, Integer> enableAll() {
        int count = emoticonService.enableAllRule();
        return Map.of("count", count);
    }

    @PostMapping("/emoticon-rules/disable-all")
    public Map<String, Integer> disableAll() {
        int count = emoticonService.disableAllRule();
        return Map.of("count", count);
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

    @PostMapping("/emoticon-images/upload")
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
