package com.ensam.ma.repository;

import com.ensam.ma.model.Enrollment;
import com.ensam.ma.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Enrollment entity
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // Find enrollment by student and course
    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    // Find all enrollments for a student
    List<Enrollment> findByStudentIdOrderByEnrolledAtDesc(Long studentId);

    // Find all enrollments for a course
    List<Enrollment> findByCourseIdOrderByEnrolledAtDesc(Long courseId);

    // Find enrollments pending validation
    List<Enrollment> findByStatusOrderByEnrolledAtDesc(EnrollmentStatus status);

    // Find pending validations for courses created by an admin
    @Query("SELECT e FROM Enrollment e WHERE e.status = 'PENDING_VALIDATION' AND e.course.createdBy.id = :adminId ORDER BY e.enrolledAt DESC")
    List<Enrollment> findPendingValidationsForAdmin(@Param("adminId") Long adminId);

    // Count pending validations for admin
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.status = 'PENDING_VALIDATION' AND e.course.createdBy.id = :adminId")
    long countPendingValidationsForAdmin(@Param("adminId") Long adminId);

    // Check if student is enrolled in course
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
}
