package com.ensam.platform.repository;

import com.ensam.platform.domain.Quiz;
import com.ensam.platform.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByStudent(User student);
}
