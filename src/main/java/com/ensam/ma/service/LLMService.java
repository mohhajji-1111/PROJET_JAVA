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
 * LLM Service with Anti-Pattern Distractor Prompt
 * Uses Groq API with strict RAG enforcement
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LLMService {

    private final AIConfig aiConfig;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Groq API endpoint (faster, more reliable)
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    // Current Groq models (Dec 2024)
    private static final String[] GROQ_MODELS = { "llama-3.3-70b-versatile", "llama3-70b-8192", "gemma2-9b-it" };

    public List<QuizQuestion> generateQuiz(String courseContent, int questionCount, DifficultyLevel difficulty) {
        log.info("=== LLM SERVICE: Generating {} {} questions ===", questionCount, difficulty);

        String apiKey = aiConfig.getApi().getKey();

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
            throw new AIGenerationException("API key not configured");
        }

        if (courseContent == null || courseContent.trim().isEmpty()) {
            throw new AIGenerationException("Course content is empty - cannot generate quiz");
        }

        log.info("Course content: {} characters", courseContent.length());

        try {
            String prompt = buildAntiPatternPrompt(courseContent, questionCount, difficulty);
            String response = callLLMAPI(apiKey, prompt);
            List<QuizQuestion> questions = parseQuizResponse(response);

            log.info("SUCCESS: Generated {} high-quality questions", questions.size());
            return questions;

        } catch (AIGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM generation failed: {}", e.getMessage());
            throw new AIGenerationException("AI quiz generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * ANTI-PATTERN PROMPT: Forces unique, plausible distractors
     * Strictly uses ONLY the provided RAG context
     */
    private String buildAntiPatternPrompt(String courseContent, int questionCount, DifficultyLevel difficulty) {
        String difficultyGuidance = switch (difficulty) {
            case EASY -> "Basic recall questions. Test direct facts from the text.";
            case MEDIUM -> "Application questions. Test understanding of concepts.";
            case HARD -> "Analysis questions. Test ability to synthesize information.";
        };

        return String.format("""
                You are an expert educational assessment creator.

                === COURSE CONTENT (USE ONLY THIS) ===
                %s
                === END OF COURSE CONTENT ===

                Generate exactly %d multiple-choice questions. Difficulty: %s
                %s

                ══════════════════════════════════════════
                CRITICAL RULES FOR DISTRACTORS (WRONG ANSWERS):
                ══════════════════════════════════════════

                ❌ FORBIDDEN PATTERNS - NEVER USE THESE:
                • "It is unrelated to..."
                • "It contradicts..."
                • "It is deprecated..."
                • "This is not supported..."
                • "None of the above"
                • Generic phrases like "Option A" or "Topic 1"

                ✓ REQUIRED FOR EACH WRONG ANSWER:
                • Must be a PLAUSIBLE technical statement
                • Must be written in the SAME style as the correct answer
                • Must represent a COMMON MISCONCEPTION or related concept
                • Must be UNIQUE (no two distractors should be similar)

                ✓ EXAMPLE OF GOOD DISTRACTORS:
                If the question is about "Spring Boot Auto-Configuration":
                • GOOD: "Requires explicit XML configuration for each bean"
                • GOOD: "Only works with embedded Jetty server"
                • BAD: "It is unrelated to configuration"
                • BAD: "This feature is deprecated"

                ══════════════════════════════════════════

                VARY the correct answer position (0, 1, 2, or 3) across questions.

                Return ONLY this JSON format (no markdown, no explanation):
                {
                  "questions": [
                    {
                      "questionText": "Specific question based on course content?",
                      "options": [
                        "First plausible option",
                        "Second plausible option",
                        "Third plausible option",
                        "Fourth plausible option"
                      ],
                      "correctOption": 0,
                      "explanation": "Brief explanation citing the course content"
                    }
                  ]
                }
                """, courseContent, questionCount, difficulty, difficultyGuidance);
    }

    private String callLLMAPI(String apiKey, String prompt) throws Exception {
        Exception lastException = null;

        for (String model : GROQ_MODELS) {
            try {
                log.info("Trying model: {}", model);
                return callGroqWithModel(apiKey, prompt, model);
            } catch (Exception e) {
                log.warn("Model {} failed: {}", model, e.getMessage());
                lastException = e;
            }
        }

        throw new AIGenerationException("All LLM models failed. Last error: " +
                (lastException != null ? lastException.getMessage() : "Unknown"));
    }

    private String callGroqWithModel(String apiKey, String prompt, String model) throws Exception {
        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content",
                                "You are an expert quiz generator. Output ONLY valid JSON. Never use lazy distractor patterns."),
                        Map.of(
                                "role", "user",
                                "content", prompt)),
                "temperature", 0.7,
                "max_tokens", 4096);

        String requestBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("API response: HTTP {}", response.statusCode());

        if (response.statusCode() != 200) {
            String errorMsg = extractErrorMessage(response.body());
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + errorMsg);
        }

        JsonNode root = objectMapper.readTree(response.body());

        if (!root.has("choices") || root.get("choices").isEmpty()) {
            throw new RuntimeException("No choices in response");
        }

        String content = root.get("choices").get(0).get("message").get("content").asText();
        log.info("Received {} characters from LLM", content.length());
        return content;
    }

    private String extractErrorMessage(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.has("error") && root.get("error").has("message")) {
                return root.get("error").get("message").asText();
            }
        } catch (Exception ignored) {
        }
        return body.length() > 200 ? body.substring(0, 200) : body;
    }

    private List<QuizQuestion> parseQuizResponse(String response) throws Exception {
        String cleaned = response.trim();

        // Remove markdown code blocks
        if (cleaned.startsWith("```json"))
            cleaned = cleaned.substring(7);
        else if (cleaned.startsWith("```"))
            cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```"))
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        cleaned = cleaned.trim();

        // Find JSON boundaries
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }

        JsonNode root = objectMapper.readTree(cleaned);
        JsonNode questionsNode = root.get("questions");

        if (questionsNode == null || !questionsNode.isArray()) {
            throw new RuntimeException("Invalid response: 'questions' array not found");
        }

        List<QuizQuestion> questions = new ArrayList<>();

        for (JsonNode qNode : questionsNode) {
            QuizQuestion q = new QuizQuestion();
            q.setQuestionText(qNode.get("questionText").asText());
            q.setCorrectOption(qNode.get("correctOption").asInt());
            q.setExplanation(qNode.has("explanation") ? qNode.get("explanation").asText() : "See course content.");

            List<String> options = new ArrayList<>();
            for (JsonNode opt : qNode.get("options")) {
                options.add(opt.asText());
            }
            q.setOptions(options);

            questions.add(q);
        }

        return questions;
    }

    public static class AIGenerationException extends RuntimeException {
        public AIGenerationException(String message) {
            super(message);
        }

        public AIGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @lombok.Data
    public static class QuizQuestion {
        private String questionText;
        private List<String> options;
        private int correctOption;
        private String explanation;
    }
}
