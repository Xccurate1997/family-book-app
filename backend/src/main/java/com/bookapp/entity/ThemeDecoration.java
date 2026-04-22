package com.bookapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "theme_decorations")
@Data
@NoArgsConstructor
public class ThemeDecoration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    @JsonIgnore
    private Theme theme;

    @Column(nullable = false)
    private String tabKey;

    private String leftImageFilename;

    private String rightImageFilename;
}
