package com.ensam.ma.repository;

import com.ensam.ma.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    
    List<QuizAttempt> findByStudentIdOrderByStartedAtDesc(Long studentId);
    
    List<QuizAttempt> findByCourseIdOrderByStartedAtDesc(Long courseId);
    
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.student.id = :studentId AND qa.course.id = :courseId ORDER BY qa.startedAt DESC")
    List<QuizAttempt> findByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.student.id = :studentId AND qa.course.id = :courseId AND qa.completed = true ORDER BY qa.startedAt DESC")
    List<QuizAttempt> findCompletedByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.student.id = :studentId AND qa.course.id = :courseId AND qa.passed = true ORDER BY qa.startedAt DESC")
    Optional<QuizAttempt> findPassedAttempt(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.completed = true")
    long countCompleted();
    
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.passed = true")
    long countPassed();
    
    @Query("SELECT AVG(qa.score) FROM QuizAttempt qa WHERE qa.completed = true")
    Double averageScore();
}
