package com.ensam.ma.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Enrollment entity to track student course enrollment and validation status
 */
@Entity
@Table(name = "enrollments", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "student_id", "course_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status = EnrollmentStatus.IN_PROGRESS;

    @Column(nullable = false)
    private LocalDateTime enrolledAt;

    private LocalDateTime validatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by_id")
    private User validatedBy; // Admin who approved

    // Highest scores achieved at each difficulty level
    private Integer easyBestScore;
    private Integer mediumBestScore;
    private Integer hardBestScore;

    @PrePersist
    protected void onCreate() {
        enrolledAt = LocalDateTime.now();
    }

    /**
     * Check if student has passed all difficulty levels with >80%
     */
    public boolean hasCompletedAllLevels() {
        return easyBestScore != null && easyBestScore >= 80 &&
                mediumBestScore != null && mediumBestScore >= 80 &&
                hardBestScore != null && hardBestScore >= 80;
    }

    /**
     * Update best score for a difficulty level
     */
    public void updateBestScore(DifficultyLevel difficulty, int score) {
        switch (difficulty) {
            case EASY -> {
                if (easyBestScore == null || score > easyBestScore) {
                    easyBestScore = score;
                }
            }
            case MEDIUM -> {
                if (mediumBestScore == null || score > mediumBestScore) {
                    mediumBestScore = score;
                }
            }
            case HARD -> {
                if (hardBestScore == null || score > hardBestScore) {
                    hardBestScore = score;
                }
            }
        }
    }

    /**
     * Get progress summary string
     */
    public String getProgressSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("EASY: ").append(easyBestScore != null ? easyBestScore + "%" : "Not attempted");
        sb.append(" | MEDIUM: ").append(mediumBestScore != null ? mediumBestScore + "%" : "Not attempted");
        sb.append(" | HARD: ").append(hardBestScore != null ? hardBestScore + "%" : "Not attempted");
        return sb.toString();
    }
}
