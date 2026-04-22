package com.bookapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "themes")
@Data
@NoArgsConstructor
public class Theme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private boolean isDefault = false;

    private String primaryColor;

    private String headerBg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SplashType splashType = SplashType.NONE;

    private String splashImageFilename;

    private int splashDuration = 2000;

    private String soundFilename;

    // ── 点缀配置 ──
    /** Header 左侧 Logo/吉祥物图片 */
    private String logoFilename;

    /** Header 背景纹理图片 */
    private String headerPatternFilename;

    /** Header 背景纹理透明度 (0~1) */
    private Double headerPatternOpacity = 0.06;

    /** 底部角落装饰图片 */
    private String cornerDecoFilename;

    /** 空状态插图 */
    private String emptyStateFilename;

    // ── 装饰图片参数 ──
    /** 两侧装饰图片透明度 (0~1) */
    private Double decoOpacity = 0.6;

    /** 两侧装饰图片最大高度 (vh) */
    private Integer decoMaxHeight = 60;

    /** 两侧装饰图片最大宽度 (px) */
    private Integer decoMaxWidth = 280;

    /** 两侧装饰图片上下渐变边缘比例 (%) */
    private Integer decoFadeEdge = 15;

    /** 两侧装饰图片左右渐变范围比例 (%) */
    private Integer decoFadeSide = 70;

    // ── 收支记录背景图 ──
    /** 收入记录行背景图 */
    private String incomeBgFilename;

    /** 支出记录行背景图 */
    private String expenseBgFilename;

    @OneToMany(mappedBy = "theme", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ThemeDecoration> decorations = new ArrayList<>();

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
