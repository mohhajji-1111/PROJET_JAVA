package com.ensam.ma.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for parsing LLM quiz response JSON
 * Maps: questionText, options, correctAnswerIndex/correctOption, explanation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponse {

    private List<QuizQuestionDTO> questions = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizQuestionDTO {

        private String questionText;

        private List<String> options = new ArrayList<>();

        // Support both naming conventions from LLM
        @JsonProperty("correctOption")
        private Integer correctOption;

        @JsonProperty("correctAnswerIndex")
        private Integer correctAnswerIndex;

        private String explanation;

        /**
         * Get the correct option index (supports both field names)
         */
        public int getCorrectOptionIndex() {
            if (correctOption != null) {
                return correctOption;
            }
            if (correctAnswerIndex != null) {
                return correctAnswerIndex;
            }
            return 0; // Default to first option
        }
    }
}
