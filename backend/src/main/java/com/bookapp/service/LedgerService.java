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

    public List<Ledger> findByUser(Long userId) {
        return ledgerRepository.findByUserId(userId);
    }

    public Ledger create(Long userId, LedgerRequest req) {
        Ledger ledger = new Ledger();
        ledger.setName(req.name());
        ledger.setIcon(req.icon());
        ledger.setColor(req.color());
        ledger.setUserId(userId);
        return ledgerRepository.save(ledger);
    }

    public Ledger update(Long userId, Long id, LedgerRequest req) {
        Ledger ledger = ledgerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("账本不存在: " + id));
        if (!ledger.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此账本");
        }
        ledger.setName(req.name());
        ledger.setIcon(req.icon());
        ledger.setColor(req.color());
        return ledgerRepository.save(ledger);
    }

    public void delete(Long userId, Long id) {
        Ledger ledger = ledgerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("账本不存在: " + id));
        if (!ledger.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此账本");
        }
        if (ledgerRepository.countByUserId(userId) <= 1) {
            throw new IllegalStateException("至少保留一个账本");
        }
        if (transactionRepository.existsByLedgerId(id)) {
            throw new IllegalStateException("该账本下存在交易记录，无法删除");
        }
        ledgerRepository.deleteById(id);
    }

    public record LedgerRequest(String name, String icon, String color) {}
}
