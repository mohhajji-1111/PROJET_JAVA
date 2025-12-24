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
    private final RAGService ragService;

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

    public List<Course> findByEnrolledStudent(Long studentId) {
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
        log.info("Created course: {} with enrollment code: {}", saved.getTitle(), saved.getEnrollmentCode());
        return saved;
    }

    public Course updateCourse(Long id, CourseDTO dto) {
        Course course = findById(id);

        // Check if content has changed before updating
        boolean contentChanged = course.getContent() != null && !course.getContent().equals(dto.getContent());

        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setContent(dto.getContent());

        // If content changes, mark as not indexed and remove old index
        if (contentChanged) {
            course.setIndexed(false);
            ragService.removeIndex(course.getId());
            log.info("Content changed, removed index for course: {}", course.getTitle());
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

        // Automatically index course content for RAG when publishing
        if (!course.isIndexed()) {
            ragService.indexCourse(course);
            course.setIndexed(true);
            log.info("Auto-indexed course during publish: {}", course.getTitle());
        }

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

    /**
     * Enroll a student in a course using the 6-character enrollment code
     * 
     * @param enrollmentCode the unique course code shared by the teacher
     * @param student        the student to enroll
     * @return the course if enrollment is successful
     * @throws RuntimeException if code is invalid, course not published, or already
     *                          enrolled
     */
    @Transactional
    public Course enrollByCode(String enrollmentCode, User student) {
        if (!student.isStudent()) {
            throw new RuntimeException("Only students can enroll using a code");
        }

        // Normalize the code (uppercase, trim whitespace)
        String normalizedCode = enrollmentCode.trim().toUpperCase();

        // Find course by enrollment code
        Course course = courseRepository.findByEnrollmentCode(normalizedCode)
                .orElseThrow(() -> new RuntimeException("Invalid course code: " + enrollmentCode));

        // Verify course is published
        if (!course.isPublished()) {
            throw new RuntimeException("This course is not available for enrollment");
        }

        // Check if already enrolled using repository (avoids lazy loading issues)
        if (isStudentEnrolled(course.getId(), student.getId())) {
            throw new RuntimeException("You are already enrolled in this course");
        }

        // Get fresh student from DB to avoid detached entity issues
        User freshStudent = userRepository.findById(student.getId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Enroll the student
        course.enrollStudent(freshStudent);
        Course updated = courseRepository.save(course);
        log.info("Student {} enrolled in course '{}' via code {}",
                student.getUsername(), course.getTitle(), normalizedCode);
        return updated;
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
        ragService.indexCourse(course);
        course.setIndexed(true);
        courseRepository.save(course);
        log.info("Indexed and marked course {} as indexed", course.getTitle());
    }

    public long countPublished() {
        return courseRepository.countPublished();
    }

    public long countIndexed() {
        return courseRepository.countIndexed();
    }
}
