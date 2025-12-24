package com.ensam.ma.model;

import jakarta.persistence.*;
import lombok.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Course entity representing a pedagogical course
 * Contains text content for RAG indexing
 * Generates unique 6-character enrollment code on creation
 */
@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    // Characters for enrollment code (excludes ambiguous: 0/O, 1/I/L)
    private static final String ALPHANUMERIC = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, unique = true, length = 6)
    private String enrollmentCode;

    @Column(nullable = false)
    private boolean published = false;

    @Column(nullable = false)
    private boolean indexed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "course_enrollments", joinColumns = @JoinColumn(name = "course_id"), inverseJoinColumns = @JoinColumn(name = "student_id"))
    @Builder.Default
    private Set<User> enrolledStudents = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuizAttempt> quizAttempts = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (enrollmentCode == null) {
            enrollmentCode = generateEnrollmentCode();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Generates a unique 6-character alphanumeric enrollment code
     */
    private String generateEnrollmentCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    public void enrollStudent(User student) {
        if (student.getRole() == Role.STUDENT) {
            this.enrolledStudents.add(student);
            student.getEnrolledCourses().add(this);
        }
    }

    public void unenrollStudent(User student) {
        this.enrolledStudents.remove(student);
        student.getEnrolledCourses().remove(this);
    }

    public boolean isStudentEnrolled(User student) {
        return enrolledStudents.contains(student);
    }

    public boolean canGenerateQuiz() {
        return published && indexed && content != null && !content.trim().isEmpty();
    }
}
