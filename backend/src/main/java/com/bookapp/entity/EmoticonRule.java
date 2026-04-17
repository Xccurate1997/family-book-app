package com.bookapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "emoticon_rules")
@Data
@NoArgsConstructor
public class EmoticonRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** TEXT or IMAGE */
    @Column(nullable = false)
    private String emojiType;

    /** emoji 字符串（TEXT）或图片文件名（IMAGE） */
    @Column(nullable = false)
    private String emojiContent;

    /** null 表示匹配所有类型 */
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

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
