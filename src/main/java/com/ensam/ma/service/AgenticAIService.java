package com.ensam.ma.service;

import com.ensam.ma.config.AgentConfig;
import com.ensam.ma.model.*;
import com.ensam.ma.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agentic AI Service - Intelligent supervisor for quiz generation and evaluation
 * This is the crown jewel of the system - an autonomous agent that makes pedagogical decisions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgenticAIService {
    
    private final AgentConfig agentConfig;
    private final QuizAttemptRepository quizAttemptRepository;
    private final RAGService ragService;
    private final LLMService llmService;
    
    /**
     * Agent determines optimal quiz difficulty based on student's history
     */
    public DifficultyLevel determineDifficulty(User student, Course course) {
        log.info("Agent analyzing difficulty for student {} on course {}", student.getUsername(), course.getId());
        
        List<QuizAttempt> history = quizAttemptRepository.findByStudentAndCourse(student.getId(), course.getId());
        
        if (history.isEmpty()) {
            log.info("First attempt - agent selects EASY difficulty");
            return DifficultyLevel.EASY;
        }
        
        // Analyze last 3 attempts
        List<QuizAttempt> recentAttempts = history.stream()
            .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()))
            .limit(3)
            .toList();
        
        double avgScore = recentAttempts.stream()
            .filter(QuizAttempt::isCompleted)
            .mapToInt(QuizAttempt::getScore)
            .average()
            .orElse(0);
        
        DifficultyLevel decision;
        if (avgScore >= 80) {
            decision = DifficultyLevel.HARD;
            log.info("Agent escalates to HARD (avg score: {}%)", avgScore);
        } else if (avgScore >= 60) {
            decision = DifficultyLevel.MEDIUM;
            log.info("Agent maintains MEDIUM (avg score: {}%)", avgScore);
        } else {
            decision = DifficultyLevel.EASY;
            log.info("Agent simplifies to EASY (avg score: {}%)", avgScore);
        }
        
        return decision;
    }
    
    /**
     * Agent orchestrates quiz generation with RAG and LLM
     */
    public QuizAttempt generateQuiz(User student, Course course) {
        log.info("Agent orchestrating quiz generation for student {} on course {}", 
                 student.getUsername(), course.getId());
        
        // 1. Determine difficulty
        DifficultyLevel difficulty = determineDifficulty(student, course);
        
        // 2. Select question count
        int questionCount = agentConfig.getQuestionCountForDifficulty(difficulty);
        
        // 3. Verify course is indexed for RAG
        if (!ragService.isCourseIndexed(course.getId())) {
            log.info("Agent triggers RAG indexing for course {}", course.getId());
            ragService.indexCourse(course);
        }
        
        // 4. Retrieve relevant context via RAG
        String context = ragService.retrieveFullContext(course.getId());
        if (context.isEmpty()) {
            log.error("Agent cannot retrieve context for course {}", course.getId());
            throw new IllegalStateException("No content available for quiz generation");
        }
        
        // 5. Generate questions via LLM
        List<LLMService.QuizQuestion> llmQuestions = llmService.generateQuiz(context, questionCount, difficulty);
        
        // 6. Create quiz attempt entity
        QuizAttempt attempt = new QuizAttempt();
        attempt.setStudent(student);
        attempt.setCourse(course);
        attempt.setDifficulty(difficulty);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setTotalQuestions(llmQuestions.size());
        attempt.setCompleted(false);
        attempt.setPassed(false);
        
        // Store agent's decision reasoning
        attempt.setAgentDecision(String.format(
            "Agent Decision: Difficulty=%s based on student history. Questions=%d. RAG context retrieved: %d chars.",
            difficulty, questionCount, context.length()
        ));
        
        // 7. Convert LLM questions to domain entities
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
        
        log.info("Agent generated quiz: {} questions at {} difficulty", llmQuestions.size(), difficulty);
        
        return attempt;
    }
    
    /**
     * Agent evaluates quiz and makes pass/fail decision
     */
    public EvaluationResult evaluateQuiz(QuizAttempt attempt) {
        log.info("Agent evaluating quiz attempt {}", attempt.getId());
        
        if (!attempt.isCompleted()) {
            throw new IllegalStateException("Cannot evaluate incomplete quiz");
        }
        
        // Calculate score
        int totalQuestions = attempt.getTotalQuestions();
        int correctAnswers = attempt.getCorrectAnswers();
        int score = (correctAnswers * 100) / totalQuestions;
        
        // Apply pass threshold
        boolean passed = score >= agentConfig.getPassThreshold();
        
        // Generate personalized feedback
        String feedback = generateFeedback(score, attempt.getDifficulty(), passed);
        
        // Make pedagogical decision
        String decision = makeDecision(score, attempt.getDifficulty(), passed);
        
        log.info("Agent evaluation: Score={}%, Passed={}, Difficulty={}", score, passed, attempt.getDifficulty());
        
        EvaluationResult result = new EvaluationResult();
        result.setScore(score);
        result.setPassed(passed);
        result.setFeedback(feedback);
        result.setDecision(decision);
        
        return result;
    }
    
    private String generateFeedback(int score, DifficultyLevel difficulty, boolean passed) {
        if (score >= 90) {
            return "🌟 Excellent work! You've mastered this content at " + difficulty + " level. " +
                   "The AI agent is impressed with your understanding!";
        } else if (score >= 80) {
            return "👍 Great job! You have a strong grasp of the material at " + difficulty + " level. " +
                   "The agent notes some minor areas for review.";
        } else if (score >= 70) {
            return "✅ Good effort! You've passed at " + difficulty + " level. " +
                   "The agent recommends reviewing the concepts you missed for deeper understanding.";
        } else if (score >= 60) {
            return "📚 You're close to passing! Review the course content, especially areas covered by questions you missed. " +
                   "The AI agent will adjust your next quiz difficulty to help you succeed.";
        } else {
            return "💡 Don't be discouraged! Learning takes time. Focus on understanding the core concepts. " +
                   "The agent has identified areas where additional study would be beneficial. Try again when ready!";
        }
    }
    
    private String makeDecision(int score, DifficultyLevel difficulty, boolean passed) {
        if (passed) {
            if (score >= 90 && difficulty == DifficultyLevel.HARD) {
                return "COURSE VALIDATED - Exceptional performance at highest difficulty!";
            } else if (score >= 80 && difficulty == DifficultyLevel.HARD) {
                return "COURSE VALIDATED - Strong performance at advanced level!";
            } else {
                return "PASSED - Continue to build on this foundation!";
            }
        } else {
            if (difficulty == DifficultyLevel.EASY) {
                return "CONTINUE LEARNING - Review basic concepts thoroughly before retrying";
            } else {
                return "CONTINUE LEARNING - Agent will adjust difficulty in next attempt";
            }
        }
    }
    
    /**
     * Evaluation result data structure
     */
    @lombok.Data
    public static class EvaluationResult {
        private int score;
        private boolean passed;
        private String feedback;
        private String decision;
    }
}
