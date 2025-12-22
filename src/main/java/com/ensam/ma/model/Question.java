package com.ensam.ma.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Question entity representing a single quiz question
 */
@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_attempt_id", nullable = false)
    private QuizAttempt quizAttempt;
    
    @Column(nullable = false)
    private int questionNumber;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;
    
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("optionNumber ASC")
    private List<QuestionOption> options = new ArrayList<>();
    
    @Column(columnDefinition = "TEXT")
    private String explanation;
    
    private Integer selectedOptionNumber;
    
    @Column(nullable = false)
    private int correctOptionNumber;
    
    public void addOption(QuestionOption option) {
        options.add(option);
        option.setQuestion(this);
    }
    
    public boolean isAnswered() {
        return selectedOptionNumber != null;
    }
    
    public boolean isAnsweredCorrectly() {
        return selectedOptionNumber != null && selectedOptionNumber == correctOptionNumber;
    }
    
    public QuestionOption getCorrectOption() {
        return options.stream()
            .filter(opt -> opt.getOptionNumber() == correctOptionNumber)
            .findFirst()
            .orElse(null);
    }
    
    public QuestionOption getSelectedOption() {
        if (selectedOptionNumber == null) return null;
        return options.stream()
            .filter(opt -> opt.getOptionNumber() == selectedOptionNumber)
            .findFirst()
            .orElse(null);
    }
}
