package com.bookapp.controller;

import com.bookapp.entity.Ledger;
import com.bookapp.service.LedgerService;
import org.springframework.http.ResponseEntity;
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
    public List<Ledger> getAll() {
        return ledgerService.findAll();
    }

    @PostMapping
    public Ledger create(@RequestBody LedgerService.LedgerRequest req) {
        return ledgerService.create(req);
    }

    @PutMapping("/{id}")
    public Ledger update(@PathVariable Long id, @RequestBody LedgerService.LedgerRequest req) {
        return ledgerService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            ledgerService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
