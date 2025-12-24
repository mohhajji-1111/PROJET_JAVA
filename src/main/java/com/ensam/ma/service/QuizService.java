package com.ensam.ma.service;

import com.ensam.ma.model.*;
import com.ensam.ma.repository.CourseRepository;
import com.ensam.ma.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Quiz Service - Manages quiz lifecycle and student interactions
 * Integrates with AI Agent for adaptive quiz generation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final AgenticAIService agenticAIService;
    private final QuizAttemptRepository quizAttemptRepository;
    private final CourseRepository courseRepository;
    private final RAGService ragService;
    private final LLMService llmService;

    // Difficulty thresholds
    private static final double HIGH_SCORE_THRESHOLD = 80.0;
    private static final double LOW_SCORE_THRESHOLD = 50.0;

    // Time limits in seconds
    private static final int TIME_LIMIT_HARD = 300; // 5 minutes
    private static final int TIME_LIMIT_MEDIUM = 600; // 10 minutes
    private static final int TIME_LIMIT_EASY = 900; // 15 minutes

    /**
     * Generate a new quiz for a student
     * AI Agent determines difficulty based on student's history
     */
    @Transactional
    public QuizAttempt generateQuiz(User student, Course course) {
        log.info("=== AI AGENT: Starting quiz generation ===");
        log.info("Student: {}, Course: {}", student.getUsername(), course.getTitle());

        // Step 1: Verify student is enrolled using repository
        if (!courseRepository.isStudentEnrolled(course.getId(), student.getId())) {
            throw new IllegalStateException("Student not enrolled in this course");
        }

        if (!course.isPublished()) {
            throw new IllegalStateException("Cannot generate quiz for unpublished course");
        }

        // Step 2: AI Agent analyzes student history to determine difficulty
        DifficultyLevel difficulty = determineAdaptiveDifficulty(student.getId(), course.getId());
        int timeLimit = getTimeLimitForDifficulty(difficulty);

        log.info("AI Agent Decision: Difficulty={}, TimeLimit={}s", difficulty, timeLimit);

        // Step 3: Ensure course is indexed in RAG system
        if (!ragService.isCourseIndexed(course.getId())) {
            log.info("AI Agent: Indexing course content for RAG...");
            ragService.indexCourse(course);
        }

        // Step 4: Retrieve context from RAG
        String context = ragService.retrieveFullContext(course.getId());
        if (context.isEmpty()) {
            log.error("AI Agent: No content available for quiz generation");
            throw new IllegalStateException("No content available for quiz generation");
        }
        log.info("AI Agent: Retrieved {} characters of context from RAG", context.length());

        // Step 5: Generate questions via LLM
        int questionCount = getQuestionCountForDifficulty(difficulty);
        List<LLMService.QuizQuestion> llmQuestions = llmService.generateQuiz(context, questionCount, difficulty);
        log.info("AI Agent: Generated {} questions via LLM", llmQuestions.size());

        // Step 6: Create quiz attempt entity
        QuizAttempt attempt = new QuizAttempt();
        attempt.setStudent(student);
        attempt.setCourse(course);
        attempt.setDifficulty(difficulty);
        attempt.setTimeLimitSeconds(timeLimit);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setTotalQuestions(llmQuestions.size());
        attempt.setCompleted(false);
        attempt.setPassed(false);

        // Store agent's decision reasoning
        Double avgScore = quizAttemptRepository.findAverageScoreByStudentAndCourse(student.getId(), course.getId());
        attempt.setAgentDecision(String.format(
                "AI Agent Analysis: AvgScore=%.1f%%, Difficulty=%s, Questions=%d, TimeLimit=%ds, RAGContext=%d chars",
                avgScore != null ? avgScore : 0.0, difficulty, questionCount, timeLimit, context.length()));

        // Step 7: Convert LLM questions to domain entities
        int questionNumber = 1;
        for (LLMService.QuizQuestion llmQ : llmQuestions) {
            Question question = new Question();
            question.setQuizAttempt(attempt);
            question.setQuestionNumber(questionNumber++);
            question.setQuestionText(llmQ.getQuestionText());
            question.setCorrectOptionNumber(llmQ.getCorrectOption() + 1); // Convert 0-based to 1-based
            question.setExplanation(llmQ.getExplanation());

            // Create options
            int optionNumber = 1;
            for (String optionText : llmQ.getOptions()) {
                QuestionOption option = new QuestionOption();
                option.setQuestion(question);
                option.setOptionNumber(optionNumber++);
                option.setOptionText(optionText);
                question.addOption(option);
            }

            attempt.addQuestion(question);
        }

        // Save the attempt
        attempt = quizAttemptRepository.save(attempt);

        log.info("=== AI AGENT: Quiz generation complete ===");
        log.info("Quiz ID: {}, Questions: {}, Difficulty: {}", attempt.getId(), attempt.getTotalQuestions(),
                difficulty);

        return attempt;
    }

    /**
     * AI Agent: Determine adaptive difficulty based on student's history
     * - Avg > 80% → HARD
     * - Avg < 50% → EASY
     * - Otherwise → MEDIUM
     */
    private DifficultyLevel determineAdaptiveDifficulty(Long studentId, Long courseId) {
        log.info("AI Agent: Analyzing student history for adaptive difficulty...");

        Double avgScore = quizAttemptRepository.findAverageScoreByStudentAndCourse(studentId, courseId);
        long attemptCount = quizAttemptRepository.countCompletedByStudentAndCourse(studentId, courseId);

        log.info("AI Agent: Found {} previous attempts, Average Score: {}", attemptCount, avgScore);

        // First attempt - start with EASY
        if (avgScore == null || attemptCount == 0) {
            log.info("AI Agent: First attempt detected - selecting EASY difficulty");
            return DifficultyLevel.EASY;
        }

        // Adaptive difficulty based on performance
        if (avgScore >= HIGH_SCORE_THRESHOLD) {
            log.info("AI Agent: High performance ({}%) - escalating to HARD", avgScore);
            return DifficultyLevel.HARD;
        } else if (avgScore < LOW_SCORE_THRESHOLD) {
            log.info("AI Agent: Low performance ({}%) - simplifying to EASY", avgScore);
            return DifficultyLevel.EASY;
        } else {
            log.info("AI Agent: Moderate performance ({}%) - maintaining MEDIUM", avgScore);
            return DifficultyLevel.MEDIUM;
        }
    }

    /**
     * Get time limit based on difficulty
     */
    private int getTimeLimitForDifficulty(DifficultyLevel difficulty) {
        return switch (difficulty) {
            case EASY -> TIME_LIMIT_EASY;
            case MEDIUM -> TIME_LIMIT_MEDIUM;
            case HARD -> TIME_LIMIT_HARD;
        };
    }

    /**
     * Get question count based on difficulty
     */
    private int getQuestionCountForDifficulty(DifficultyLevel difficulty) {
        return switch (difficulty) {
            case EASY -> 5;
            case MEDIUM -> 10;
            case HARD -> 15;
        };
    }

    /**
     * Submit quiz answers and evaluate using Agentic Supervisor
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

        // Calculate score
        int score = (correctCount * 100) / questions.size();

        // Update attempt
        attempt.setCompleted(true);
        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setCorrectAnswers(correctCount);
        attempt.setScore(score);

        // Let the agentic AI evaluate and decide
        AgenticAIService.EvaluationResult evaluation = agenticAIService.evaluateQuiz(attempt);

        attempt.setPassed(evaluation.isPassed());

        // Update agent decision with validation status
        String validationStatus = evaluation.isPassed() ? "COURSE VALIDATED ✓" : "CONTINUE LEARNING - Score below 70%";
        attempt.setAgentDecision(attempt.getAgentDecision() + " | Final: " + validationStatus);

        quizAttemptRepository.save(attempt);

        log.info("=== AGENT SUPERVISOR DECISION ===");
        log.info("Score: {}%, Passed: {}, Status: {}", score, evaluation.isPassed(), validationStatus);

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

    /**
     * Get recent quiz attempts for a student (limited number)
     */
    public List<QuizAttempt> findRecentAttemptsByStudent(Long studentId, int limit) {
        List<QuizAttempt> all = quizAttemptRepository.findByStudentIdOrderByStartedAtDesc(studentId);
        return all.stream().limit(limit).toList();
    }
}
