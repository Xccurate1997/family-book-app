package com.bookapp.controller;

import com.bookapp.entity.Ledger;
import com.bookapp.service.LedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ledgers")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping
    public List<Ledger> getAll(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ledgerService.findByUser(userId);
    }

    @PostMapping
    public Ledger create(Authentication auth, @RequestBody LedgerService.LedgerRequest req) {
        Long userId = (Long) auth.getPrincipal();
        return ledgerService.create(userId, req);
    }

    @PutMapping("/{id}")
    public Ledger update(Authentication auth, @PathVariable Long id,
                         @RequestBody LedgerService.LedgerRequest req) {
        Long userId = (Long) auth.getPrincipal();
        return ledgerService.update(userId, id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        try {
            ledgerService.delete(userId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
