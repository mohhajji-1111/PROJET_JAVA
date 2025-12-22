package com.ensam.ma.service;

import com.ensam.ma.model.*;
import com.ensam.ma.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Quiz Service - Manages quiz lifecycle and student interactions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {
    
    private final AgenticAIService agenticAIService;
    private final QuizAttemptRepository quizAttemptRepository;
    
    /**
     * Generate a new quiz for a student
     */
    @Transactional
    public QuizAttempt generateQuiz(User student, Course course) {
        log.info("Generating quiz for student {} on course {}", student.getUsername(), course.getId());
        
        // Verify student is enrolled
        if (!course.getEnrolledStudents().contains(student)) {
            throw new IllegalStateException("Student not enrolled in this course");
        }
        
        if (!course.isPublished()) {
            throw new IllegalStateException("Cannot generate quiz for unpublished course");
        }
        
        // Let the agentic AI handle quiz generation
        QuizAttempt attempt = agenticAIService.generateQuiz(student, course);
        
        // Save the attempt
        attempt = quizAttemptRepository.save(attempt);
        
        log.info("Quiz generated successfully: {} questions", attempt.getTotalQuestions());
        
        return attempt;
    }
    
    /**
     * Submit quiz answers and evaluate
     */
    @Transactional
    public AgenticAIService.EvaluationResult submitQuiz(Long attemptId, List<Integer> answers) {
        log.info("Submitting quiz attempt {}", attemptId);
        
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new IllegalArgumentException("Quiz attempt not found"));
        
        if (attempt.isCompleted()) {
            throw new IllegalStateException("Quiz already completed");
        }
        
        // Record answers
        List<Question> questions = attempt.getQuestions();
        if (answers.size() != questions.size()) {
            throw new IllegalArgumentException("Answer count mismatch");
        }
        
        int correctCount = 0;
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            int selectedAnswer = answers.get(i);
            
            question.setSelectedOptionNumber(selectedAnswer);
            
            if (selectedAnswer == question.getCorrectOptionNumber()) {
                correctCount++;
            }
        }
        
        // Update attempt
        attempt.setCompleted(true);
        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setCorrectAnswers(correctCount);
        attempt.setScore((correctCount * 100) / questions.size());
        
        // Let the agentic AI evaluate and decide
        AgenticAIService.EvaluationResult evaluation = agenticAIService.evaluateQuiz(attempt);
        
        attempt.setPassed(evaluation.isPassed());
        quizAttemptRepository.save(attempt);
        
        log.info("Quiz evaluated: Score={}%, Passed={}", evaluation.getScore(), evaluation.isPassed());
        
        return evaluation;
    }
    
    /**
     * Get quiz attempt by ID
     */
    public QuizAttempt getQuizAttempt(Long attemptId) {
        return quizAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new IllegalArgumentException("Quiz attempt not found"));
    }
    
    /**
     * Get student's quiz history for a course
     */
    public List<QuizAttempt> getStudentQuizHistory(User student, Course course) {
        return quizAttemptRepository.findByStudentAndCourse(student.getId(), course.getId());
    }
    
    /**
     * Get all quiz attempts for a student
     */
    public List<QuizAttempt> getStudentAllQuizzes(User student) {
        return quizAttemptRepository.findByStudentIdOrderByStartedAtDesc(student.getId());
    }
}
