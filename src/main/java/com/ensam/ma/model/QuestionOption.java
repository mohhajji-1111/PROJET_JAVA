package com.ensam.ma.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Question option entity representing a possible answer
 */
@Entity
@Table(name = "question_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionOption {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    
    @Column(nullable = false)
    private int optionNumber;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String optionText;
}
