package com.ensam.ma.config;

import com.ensam.ma.model.DifficultyLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Agentic AI configuration properties
 */
@Configuration
@ConfigurationProperties(prefix = "agent")
@Getter
@Setter
public class AgentConfig {

    private Quiz quiz = new Quiz();
    private TimeLimit timeLimit = new TimeLimit();
    private List<String> difficultyLevels = List.of("EASY", "MEDIUM", "HARD");
    private int passThreshold = 70; // Percentage

    @Getter
    @Setter
    public static class Quiz {
        private int minQuestions = 5;
        private int maxQuestions = 15;
        private int defaultQuestions = 10;
    }

    @Getter
    @Setter
    public static class TimeLimit {
        private int easySeconds = 900; // 15 minutes for EASY
        private int mediumSeconds = 600; // 10 minutes for MEDIUM
        private int hardSeconds = 300; // 5 minutes for HARD
    }

    public int getQuestionCountForDifficulty(DifficultyLevel difficulty) {
        return switch (difficulty) {
            case EASY -> quiz.minQuestions;
            case MEDIUM -> quiz.defaultQuestions;
            case HARD -> quiz.maxQuestions;
        };
    }

    /**
     * Get time limit in seconds based on difficulty level
     */
    public int getTimeLimitForDifficulty(DifficultyLevel difficulty) {
        return switch (difficulty) {
            case EASY -> timeLimit.easySeconds;
            case MEDIUM -> timeLimit.mediumSeconds;
            case HARD -> timeLimit.hardSeconds;
        };
    }
}
