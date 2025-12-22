package com.ensam.ma.service;

import com.ensam.ma.dto.UserDTO;
import com.ensam.ma.model.Role;
import com.ensam.ma.model.User;
import com.ensam.ma.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User management service
 * Handles user CRUD operations (primarily for administrators)
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }
    
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
    
    public List<User> findAllStudents() {
        return userRepository.findAllStudents();
    }
    
    public List<User> findAllAdministrators() {
        return userRepository.findAllAdministrators();
    }
    
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    public User createStudent(UserDTO dto) {
        validateNewUser(dto);
        
        User student = User.builder()
            .username(dto.getUsername())
            .password(passwordEncoder.encode(dto.getPassword()))
            .fullName(dto.getFullName())
            .email(dto.getEmail())
            .role(Role.STUDENT)
            .enabled(true)
            .build();
        
        User saved = userRepository.save(student);
        log.info("Created student: {}", saved.getUsername());
        return saved;
    }
    
    public User createAdministrator(UserDTO dto) {
        validateNewUser(dto);
        
        User admin = User.builder()
            .username(dto.getUsername())
            .password(passwordEncoder.encode(dto.getPassword()))
            .fullName(dto.getFullName())
            .email(dto.getEmail())
            .role(Role.ADMINISTRATOR)
            .enabled(true)
            .build();
        
        User saved = userRepository.save(admin);
        log.info("Created administrator: {}", saved.getUsername());
        return saved;
    }
    
    public User updateStudent(Long id, UserDTO dto) {
        User student = findById(id);
        
        if (student.getRole() != Role.STUDENT) {
            throw new RuntimeException("User is not a student");
        }
        
        student.setFullName(dto.getFullName());
        student.setEmail(dto.getEmail());
        
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            student.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        
        User updated = userRepository.save(student);
        log.info("Updated student: {}", updated.getUsername());
        return updated;
    }
    
    public void deleteStudent(Long id) {
        User student = findById(id);
        
        if (student.getRole() != Role.STUDENT) {
            throw new RuntimeException("User is not a student");
        }
        
        userRepository.delete(student);
        log.info("Deleted student: {}", student.getUsername());
    }
    
    public void enableUser(Long id, boolean enabled) {
        User user = findById(id);
        user.setEnabled(enabled);
        userRepository.save(user);
        log.info("Set user {} enabled status to: {}", user.getUsername(), enabled);
    }
    
    private void validateNewUser(UserDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username already exists: " + dto.getUsername());
        }
        
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already exists: " + dto.getEmail());
        }
    }
    
    public long countStudents() {
        return userRepository.countStudents();
    }
    
    public long countAdministrators() {
        return userRepository.countAdministrators();
    }
}
