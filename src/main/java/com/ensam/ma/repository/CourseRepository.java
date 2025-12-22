package com.ensam.ma.repository;

import com.ensam.ma.model.Course;
import com.ensam.ma.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    
    List<Course> findByPublishedTrue();
    
    List<Course> findByIndexedTrue();
    
    List<Course> findByCreatedBy(User creator);
    
    @Query("SELECT c FROM Course c JOIN c.enrolledStudents s WHERE s.id = :studentId")
    List<Course> findByEnrolledStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT c FROM Course c JOIN c.enrolledStudents s WHERE s.id = :studentId AND c.published = true")
    List<Course> findPublishedCoursesByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT COUNT(c) FROM Course c WHERE c.published = true")
    long countPublished();
    
    @Query("SELECT COUNT(c) FROM Course c WHERE c.indexed = true")
    long countIndexed();
    
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM Course c JOIN c.enrolledStudents s WHERE c.id = :courseId AND s.id = :studentId")
    boolean isStudentEnrolled(@Param("courseId") Long courseId, @Param("studentId") Long studentId);
}
