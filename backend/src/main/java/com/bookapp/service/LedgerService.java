package com.bookapp.service;

import com.bookapp.entity.Ledger;
import com.bookapp.repository.LedgerRepository;
import com.bookapp.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LedgerService {

    private final LedgerRepository ledgerRepository;
    private final TransactionRepository transactionRepository;

    public LedgerService(LedgerRepository ledgerRepository, TransactionRepository transactionRepository) {
        this.ledgerRepository = ledgerRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Ledger> findAll() {
        return ledgerRepository.findAll();
    }

    public Ledger create(LedgerRequest req) {
        Ledger ledger = new Ledger();
        ledger.setName(req.name());
        ledger.setIcon(req.icon());
        ledger.setColor(req.color());
        return ledgerRepository.save(ledger);
    }

    public Ledger update(Long id, LedgerRequest req) {
        Ledger ledger = ledgerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("账本不存在: " + id));
        ledger.setName(req.name());
        ledger.setIcon(req.icon());
        ledger.setColor(req.color());
        return ledgerRepository.save(ledger);
    }

    public void delete(Long id) {
        if (ledgerRepository.count() <= 1) {
            throw new IllegalStateException("至少保留一个账本");
        }
        if (transactionRepository.existsByLedgerId(id)) {
            throw new IllegalStateException("该账本下存在交易记录，无法删除");
        }
        ledgerRepository.deleteById(id);
    }

    public record LedgerRequest(String name, String icon, String color) {}
}
