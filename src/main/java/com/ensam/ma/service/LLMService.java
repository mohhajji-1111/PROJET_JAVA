package com.ensam.ma.service;

import com.ensam.ma.config.AIConfig;
import com.ensam.ma.model.DifficultyLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LLM Service for generating quiz questions using OpenAI API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LLMService {
    
    private final AIConfig aiConfig;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Generate quiz questions based on course content using LLM
     */
    public List<QuizQuestion> generateQuiz(String context, int questionCount, DifficultyLevel difficulty) {
        log.info("Generating {} {} questions using LLM", questionCount, difficulty);
        
        try {
            String apiKey = aiConfig.getApi().getKey();
            
            // If API key not configured, return demo quiz
            if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
                log.warn("OpenAI API key not configured, returning demo quiz");
                return generateDemoQuiz(questionCount, difficulty);
            }
            
            String prompt = buildPrompt(context, questionCount, difficulty);
            String response = callOpenAI(apiKey, prompt);
            
            return parseQuizResponse(response);
            
        } catch (Exception e) {
            log.error("Error generating quiz with LLM, falling back to demo quiz", e);
            return generateDemoQuiz(questionCount, difficulty);
        }
    }
    
    private String buildPrompt(String context, int questionCount, DifficultyLevel difficulty) {
        return String.format("""
            You are an expert educational content creator. Generate a multiple-choice quiz based STRICTLY on the following course content.
            
            CRITICAL RULES:
            1. ALL questions MUST be derived from the provided course content only
            2. NO external knowledge or hallucinated information
            3. Each question has exactly 4 options (A, B, C, D)
            4. Only ONE option is correct
            5. Include clear explanations for correct answers
            6. Difficulty level: %s
            
            COURSE CONTENT:
            %s
            
            Generate exactly %d questions in this JSON format:
            {
              "questions": [
                {
                  "questionText": "The question text here?",
                  "options": [
                    "Option A text",
                    "Option B text",
                    "Option C text",
                    "Option D text"
                  ],
                  "correctOption": 0,
                  "explanation": "Detailed explanation of why this is correct"
                }
              ]
            }
            
            Respond ONLY with valid JSON, no additional text.
            """, difficulty, context, questionCount);
    }
    
    private String callOpenAI(String apiKey, String prompt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
            "model", aiConfig.getModel(),
            "messages", List.of(
                Map.of("role", "system", "content", "You are an expert quiz generator that creates educational assessments."),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", aiConfig.getTemperature(),
            "max_tokens", aiConfig.getMaxTokens()
        ));
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI API error: " + response.body());
        }
        
        JsonNode root = objectMapper.readTree(response.body());
        return root.get("choices").get(0).get("message").get("content").asText();
    }
    
    private List<QuizQuestion> parseQuizResponse(String response) throws Exception {
        // Clean up response if it has markdown code blocks
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();
        
        JsonNode root = objectMapper.readTree(cleaned);
        JsonNode questionsNode = root.get("questions");
        
        List<QuizQuestion> questions = new ArrayList<>();
        for (JsonNode questionNode : questionsNode) {
            QuizQuestion question = new QuizQuestion();
            question.setQuestionText(questionNode.get("questionText").asText());
            question.setCorrectOption(questionNode.get("correctOption").asInt());
            question.setExplanation(questionNode.get("explanation").asText());
            
            List<String> options = new ArrayList<>();
            JsonNode optionsNode = questionNode.get("options");
            for (JsonNode option : optionsNode) {
                options.add(option.asText());
            }
            question.setOptions(options);
            
            questions.add(question);
        }
        
        return questions;
    }
    
    private List<QuizQuestion> generateDemoQuiz(int questionCount, DifficultyLevel difficulty) {
        log.info("Generating demo quiz with {} questions", questionCount);
        
        List<QuizQuestion> questions = new ArrayList<>();
        
        // Generate demo questions based on count
        for (int i = 0; i < questionCount; i++) {
            QuizQuestion q = new QuizQuestion();
            q.setQuestionText("Demo Question " + (i + 1) + ": What is a key concept in this course?");
            q.setOptions(List.of(
                "Option A: First concept",
                "Option B: Second concept (correct)",
                "Option C: Third concept",
                "Option D: Fourth concept"
            ));
            q.setCorrectOption(1); // Option B is correct
            q.setExplanation("This is the correct answer because it directly relates to the course content. " +
                           "Option B is supported by the material covered in this section.");
            questions.add(q);
        }
        
        return questions;
    }
    
    /**
     * Quiz question data structure
     */
    @lombok.Data
    public static class QuizQuestion {
        private String questionText;
        private List<String> options;
        private int correctOption;
        private String explanation;
    }
}
