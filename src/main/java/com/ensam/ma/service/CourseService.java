package com.ensam.ma.service;

import com.ensam.ma.dto.CourseDTO;
import com.ensam.ma.model.Course;
import com.ensam.ma.model.User;
import com.ensam.ma.repository.CourseRepository;
import com.ensam.ma.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Course management service
 * Handles course CRUD, enrollment, and publishing operations
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CourseService {
    
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    
    public Course findById(Long id) {
        return courseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Course not found: " + id));
    }
    
    public List<Course> findAll() {
        return courseRepository.findAll();
    }
    
    public List<Course> findPublishedCourses() {
        return courseRepository.findByPublishedTrue();
    }
    
    public List<Course> findCoursesByStudent(Long studentId) {
        return courseRepository.findByEnrolledStudentId(studentId);
    }
    
    public List<Course> findPublishedCoursesByStudent(Long studentId) {
        return courseRepository.findPublishedCoursesByStudentId(studentId);
    }
    
    public Course createCourse(CourseDTO dto, User creator) {
        if (!creator.isAdministrator()) {
            throw new RuntimeException("Only administrators can create courses");
        }
        
        Course course = Course.builder()
            .title(dto.getTitle())
            .description(dto.getDescription())
            .content(dto.getContent())
            .published(false)
            .indexed(false)
            .createdBy(creator)
            .build();
        
        Course saved = courseRepository.save(course);
        log.info("Created course: {} by {}", saved.getTitle(), creator.getUsername());
        return saved;
    }
    
    public Course updateCourse(Long id, CourseDTO dto) {
        Course course = findById(id);
        
        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setContent(dto.getContent());
        
        // If content changes, mark as not indexed
        if (!dto.getContent().equals(course.getContent())) {
            course.setIndexed(false);
        }
        
        Course updated = courseRepository.save(course);
        log.info("Updated course: {}", updated.getTitle());
        return updated;
    }
    
    public void deleteCourse(Long id) {
        Course course = findById(id);
        courseRepository.delete(course);
        log.info("Deleted course: {}", course.getTitle());
    }
    
    public Course publishCourse(Long id) {
        Course course = findById(id);
        
        if (course.getContent() == null || course.getContent().trim().isEmpty()) {
            throw new RuntimeException("Cannot publish course without content");
        }
        
        course.setPublished(true);
        Course published = courseRepository.save(course);
        log.info("Published course: {}", published.getTitle());
        return published;
    }
    
    public Course unpublishCourse(Long id) {
        Course course = findById(id);
        course.setPublished(false);
        Course unpublished = courseRepository.save(course);
        log.info("Unpublished course: {}", unpublished.getTitle());
        return unpublished;
    }
    
    public Course enrollStudent(Long courseId, Long studentId) {
        Course course = findById(courseId);
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
        
        if (!student.isStudent()) {
            throw new RuntimeException("User is not a student");
        }
        
        course.enrollStudent(student);
        Course updated = courseRepository.save(course);
        log.info("Enrolled student {} in course {}", student.getUsername(), course.getTitle());
        return updated;
    }
    
    public Course unenrollStudent(Long courseId, Long studentId) {
        Course course = findById(courseId);
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
        
        course.unenrollStudent(student);
        Course updated = courseRepository.save(course);
        log.info("Unenrolled student {} from course {}", student.getUsername(), course.getTitle());
        return updated;
    }
    
    public boolean isStudentEnrolled(Long courseId, Long studentId) {
        return courseRepository.isStudentEnrolled(courseId, studentId);
    }
    
    public void markAsIndexed(Long courseId) {
        Course course = findById(courseId);
        course.setIndexed(true);
        courseRepository.save(course);
        log.info("Marked course {} as indexed", course.getTitle());
    }
    
    public long countPublished() {
        return courseRepository.countPublished();
    }
    
    public long countIndexed() {
        return courseRepository.countIndexed();
    }
}
