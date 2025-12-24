package com.ensam.ma.service;

import com.ensam.ma.model.*;
import com.ensam.ma.repository.EnrollmentRepository;
import com.ensam.ma.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * AGENTIC QUIZ SUPERVISOR - The Intelligent Decision Maker
 * 
 * This is the "Brain" of the system. It makes ALL decisions about:
 * 1. Quiz parameters (difficulty, question count, time limit)
 * 2. Result evaluation (scoring, time calculation)
 * 3. Course validation recommendation (PENDING_VALIDATION when all levels
 * passed)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AgenticQuizSupervisor {

    private final QuizAttemptRepository quizAttemptRepository;
    private final EnrollmentRepository enrollmentRepository;

    // Thresholds for adaptive difficulty
    private static final double HIGH_PERFORMANCE_THRESHOLD = 80.0;
    private static final double LOW_PERFORMANCE_THRESHOLD = 50.0;

    // Passing threshold for course validation
    private static final double VALIDATION_THRESHOLD = 70.0;

    // Required score per difficulty for validation recommendation
    private static final int VALIDATION_SCORE_REQUIRED = 80;

    /**
     * DECISION 1: Calculate Quiz Parameters
     * Analyzes student history and determines difficulty, question count, and time
     * limit
     */
    public QuizParameters calculateParameters(User student, Course course) {
        log.info("=== AGENT SUPERVISOR: Calculating quiz parameters ===");
        log.info("Student: {}, Course: {}", student.getUsername(), course.getTitle());

        // Query historical performance
        Double avgScore = quizAttemptRepository.findAverageScoreByStudentAndCourse(
                student.getId(), course.getId());
        long attemptCount = quizAttemptRepository.countCompletedByStudentAndCourse(
                student.getId(), course.getId());

        log.info("Agent Analysis: {} previous attempts, Average Score: {}",
                attemptCount, avgScore != null ? String.format("%.1f%%", avgScore) : "N/A");

        // Determine difficulty based on performance
        DifficultyLevel difficulty;
        int questionCount;
        int timeLimitSeconds;

        if (avgScore == null || attemptCount == 0) {
            difficulty = DifficultyLevel.EASY;
            questionCount = 5;
            timeLimitSeconds = 900;
            log.info("Agent Decision: First attempt → EASY difficulty");
        } else if (avgScore >= HIGH_PERFORMANCE_THRESHOLD) {
            difficulty = DifficultyLevel.HARD;
            questionCount = 7;
            timeLimitSeconds = 420;
            log.info("Agent Decision: High performance → HARD difficulty");
        } else if (avgScore < LOW_PERFORMANCE_THRESHOLD) {
            difficulty = DifficultyLevel.EASY;
            questionCount = 4;
            timeLimitSeconds = 1200;
            log.info("Agent Decision: Low performance → EASY difficulty");
        } else {
            difficulty = DifficultyLevel.MEDIUM;
            questionCount = 5;
            timeLimitSeconds = 600;
            log.info("Agent Decision: Average performance → MEDIUM difficulty");
        }

        return new QuizParameters(difficulty, questionCount, timeLimitSeconds);
    }

    /**
     * DECISION 2: Evaluate Quiz Results and Update Enrollment
     * Scores the quiz, updates best scores, and checks for validation eligibility
     */
    public EvaluationResult evaluateResults(QuizAttempt attempt) {
        log.info("=== AGENT SUPERVISOR: Evaluating quiz results ===");

        int score = attempt.getScore();
        boolean passed = score >= VALIDATION_THRESHOLD;

        // Calculate duration
        String durationString = attempt.getDurationString();
        log.info("Duration: {}", durationString);

        // Update enrollment best scores
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(attempt.getStudent().getId(), attempt.getCourse().getId())
                .orElse(null);

        String validationMessage = "";

        if (enrollment != null) {
            // Update best score for this difficulty
            enrollment.updateBestScore(attempt.getDifficulty(), score);

            // Check if all levels completed with 80%+
            if (enrollment.hasCompletedAllLevels() && enrollment.getStatus() == EnrollmentStatus.IN_PROGRESS) {
                enrollment.setStatus(EnrollmentStatus.PENDING_VALIDATION);
                validationMessage = " 🎯 ALL LEVELS COMPLETE! Awaiting admin validation.";
                log.info("Agent Decision: Setting status to PENDING_VALIDATION");
            }

            enrollmentRepository.save(enrollment);
        }

        // Generate agent message
        String agentMessage;
        if (score >= 90) {
            agentMessage = "EXCELLENT! " + durationString + " - Outstanding mastery demonstrated." + validationMessage;
        } else if (score >= VALIDATION_THRESHOLD) {
            agentMessage = "GOOD WORK! " + durationString + " - Competency level achieved." + validationMessage;
        } else if (score >= 50) {
            agentMessage = "NEEDS IMPROVEMENT. " + durationString + " - Review the material and retry.";
        } else {
            agentMessage = "STUDY REQUIRED. " + durationString + " - Please thoroughly review the content.";
        }

        return new EvaluationResult(score, passed, agentMessage, durationString);
    }

    /**
     * Admin approves validation
     */
    public void approveValidation(Long enrollmentId, User admin) {
        log.info("=== AGENT SUPERVISOR: Admin validation approval ===");

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));

        if (enrollment.getStatus() != EnrollmentStatus.PENDING_VALIDATION) {
            throw new IllegalStateException("Enrollment is not pending validation");
        }

        enrollment.setStatus(EnrollmentStatus.VALIDATED);
        enrollment.setValidatedAt(LocalDateTime.now());
        enrollment.setValidatedBy(admin);

        enrollmentRepository.save(enrollment);

        log.info("Course '{}' VALIDATED for student '{}' by admin '{}'",
                enrollment.getCourse().getTitle(),
                enrollment.getStudent().getUsername(),
                admin.getUsername());
    }

    /**
     * Get enrollment for student/course
     */
    public Enrollment getEnrollment(Long studentId, Long courseId) {
        return enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId).orElse(null);
    }

    // ==================== DATA CLASSES ====================

    public record QuizParameters(
            DifficultyLevel difficulty,
            int questionCount,
            int timeLimitSeconds) {
    }

    public record EvaluationResult(
            int score,
            boolean passed,
            String agentMessage,
            String duration) {
    }
}
