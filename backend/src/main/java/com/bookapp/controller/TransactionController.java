package com.bookapp.controller;

import com.bookapp.entity.Transaction;
import com.bookapp.service.TransactionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bookapp.repository.TransactionRepository;
import com.bookapp.service.EmoticonService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final EmoticonService emoticonService;

    public TransactionController(TransactionService transactionService,
                                  TransactionRepository transactionRepository,
                                  EmoticonService emoticonService) {
        this.transactionService = transactionService;
        this.transactionRepository = transactionRepository;
        this.emoticonService = emoticonService;
    }

    @GetMapping
    public List<Transaction> getByMonth(
            @RequestParam Long ledgerId,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {

        boolean hasFilter = (keyword != null && !keyword.isBlank())
                || categoryId != null || minAmount != null || maxAmount != null;

        if (hasFilter) {
            return transactionService.findFiltered(ledgerId, year, month,
                    keyword, categoryId, minAmount, maxAmount);
        }
        return transactionService.findByMonth(ledgerId, year, month);
    }

    @GetMapping("/summary")
    public Map<String, BigDecimal> getSummary(
            @RequestParam Long ledgerId,
            @RequestParam int year,
            @RequestParam int month) {
        return transactionService.getSummary(ledgerId, year, month);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam Long ledgerId,
            @RequestParam int year,
            @RequestParam int month) {
        byte[] csv = transactionService.exportToCsv(ledgerId, year, month);
        String filename = String.format("transactions-%d-%02d.csv", year, month);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/by-date")
    public List<Transaction> getByDate(
            @RequestParam Long ledgerId,
            @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        List<Transaction> list = transactionRepository.findByLedgerIdAndTransactionDateOrderByCreatedAtDesc(
                ledgerId, localDate);
        emoticonService.fillMoodEmoji(list);
        return list;
    }

    @PostMapping
    public Transaction create(@RequestBody TransactionService.TransactionRequest request) {
        return transactionService.create(request);
    }

    @PutMapping("/{id}")
    public Transaction update(@PathVariable Long id,
                               @RequestBody TransactionService.TransactionRequest request) {
        return transactionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
