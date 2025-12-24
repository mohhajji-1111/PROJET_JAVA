package com.ensam.ma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper DTO for quiz submission
 * Captures radio button values from the quiz form
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmissionWrapper {

    /**
     * List of selected answer option numbers (as strings from form)
     * Index corresponds to question index in the quiz
     */
    private List<String> answers = new ArrayList<>();

    /**
     * Convert string answers to integer list for processing
     */
    public List<Integer> getAnswersAsIntegers() {
        List<Integer> intAnswers = new ArrayList<>();
        for (String answer : answers) {
            if (answer != null && !answer.isEmpty()) {
                try {
                    intAnswers.add(Integer.parseInt(answer));
                } catch (NumberFormatException e) {
                    intAnswers.add(0); // Default to 0 if parsing fails
                }
            } else {
                intAnswers.add(0); // Default for unanswered
            }
        }
        return intAnswers;
    }
}
