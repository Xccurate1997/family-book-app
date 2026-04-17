package com.bookapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ledger_id", nullable = true)
    private Ledger ledger;

    private String description;

    @Column(nullable = false)
    private LocalDate transactionDate;

    private LocalDateTime createdAt;

    @Transient
    private Map<String, String> moodEmoji;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
