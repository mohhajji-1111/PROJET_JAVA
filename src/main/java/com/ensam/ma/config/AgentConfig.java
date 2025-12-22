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
    private List<String> difficultyLevels = List.of("EASY", "MEDIUM", "HARD");
    private int passThreshold = 70; // Percentage
    
    @Getter
    @Setter
    public static class Quiz {
        private int minQuestions = 5;
        private int maxQuestions = 15;
        private int defaultQuestions = 10;
    }
    
    public int getQuestionCountForDifficulty(DifficultyLevel difficulty) {
        return switch (difficulty) {
            case EASY -> quiz.minQuestions;
            case MEDIUM -> quiz.defaultQuestions;
            case HARD -> quiz.maxQuestions;
        };
    }
}
