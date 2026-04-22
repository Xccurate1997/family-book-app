package com.bookapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "effect_rules")
@Data
@NoArgsConstructor
public class EffectRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EffectType effectType;

    /** 当 effectType=VIDEO 时的视频文件名，可为 null */
    private String videoFilename;

    /** 匹配的交易类型，null 表示不限 */
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    /** 精确匹配金额（如 520），null 不限 */
    private BigDecimal exactAmount;

    /** 金额下限（含），null 不限 */
    private BigDecimal minAmount;

    /** 金额上限（不含），null 不限 */
    private BigDecimal maxAmount;

    /** 指定分类 ID，null 不限 */
    private Long categoryId;

    /** 优先级，越大越优先 */
    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean enabled = true;
}
