package com.ensam.platform.repository;

import com.ensam.platform.domain.Course;
import com.ensam.platform.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByEnrolledStudentsContains(User student);
}
