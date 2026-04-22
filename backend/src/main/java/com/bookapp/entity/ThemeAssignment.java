package com.bookapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "theme_assignments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"theme_id", "user_id"})
})
@Data
@NoArgsConstructor
public class ThemeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "theme_id", nullable = false)
    private Long themeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
