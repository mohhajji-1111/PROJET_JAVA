package com.ensam.ma.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Quiz attempt entity representing a student's quiz session
 * Includes time limit set by the AI agent based on difficulty
 */
@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @OneToMany(mappedBy = "quizAttempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionNumber ASC")
    private List<Question> questions = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DifficultyLevel difficulty;

    @Column(nullable = false)
    private int totalQuestions;

    /**
     * Time limit in seconds set by the AI agent based on difficulty
     * EASY: 900s (15 min), MEDIUM: 600s (10 min), HARD: 300s (5 min)
     */
    @Column(nullable = false)
    private int timeLimitSeconds = 600;

    private Integer score; // Percentage (0-100)

    private Integer correctAnswers;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(nullable = false)
    private boolean passed = false;

    @Column(columnDefinition = "TEXT")
    private String agentDecision; // AI agent's pedagogical decision

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
    }

    public void addQuestion(Question question) {
        questions.add(question);
        question.setQuizAttempt(this);
    }

    public void calculateScore() {
        if (questions.isEmpty()) {
            this.score = 0;
            this.correctAnswers = 0;
            return;
        }

        long correct = questions.stream()
                .filter(Question::isAnsweredCorrectly)
                .count();

        this.correctAnswers = (int) correct;
        this.score = (int) ((correct * 100.0) / questions.size());
    }

    /**
     * Get time limit formatted as MM:SS for display
     */
    public String getFormattedTimeLimit() {
        int minutes = timeLimitSeconds / 60;
        int seconds = timeLimitSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Calculate duration in seconds (submitTime - startTime)
     */
    public long getDurationSeconds() {
        if (startedAt == null || completedAt == null) {
            return 0;
        }
        return java.time.Duration.between(startedAt, completedAt).getSeconds();
    }

    /**
     * Get duration formatted as "X min Y sec"
     */
    public String getDurationString() {
        long totalSeconds = getDurationSeconds();
        if (totalSeconds <= 0) {
            return "N/A";
        }
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        if (minutes > 0) {
            return String.format("%d min %d sec", minutes, seconds);
        } else {
            return String.format("%d sec", seconds);
        }
    }
}
