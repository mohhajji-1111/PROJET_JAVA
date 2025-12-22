package com.ensam.ma.repository;

import com.ensam.ma.model.Role;
import com.ensam.ma.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    List<User> findByRole(Role role);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.role = 'STUDENT'")
    List<User> findAllStudents();
    
    @Query("SELECT u FROM User u WHERE u.role = 'ADMINISTRATOR'")
    List<User> findAllAdministrators();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'STUDENT'")
    long countStudents();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'ADMINISTRATOR'")
    long countAdministrators();
}
