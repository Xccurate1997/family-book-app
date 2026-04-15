package com.bookapp.controller;

import com.bookapp.entity.Transaction;
import com.bookapp.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<Transaction> getByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        return transactionService.findByMonth(year, month);
    }

    @GetMapping("/summary")
    public Map<String, BigDecimal> getSummary(
            @RequestParam int year,
            @RequestParam int month) {
        return transactionService.getSummary(year, month);
    }

    @PostMapping
    public Transaction create(@RequestBody TransactionService.TransactionRequest request) {
        return transactionService.create(request);
    }

    @PutMapping("/{id}")
    public Transaction update(
            @PathVariable Long id,
            @RequestBody TransactionService.TransactionRequest request) {
        return transactionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
