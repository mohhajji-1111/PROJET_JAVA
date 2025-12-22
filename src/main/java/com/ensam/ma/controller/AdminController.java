package com.ensam.ma.controller;

import com.ensam.ma.dto.CourseDTO;
import com.ensam.ma.dto.UserDTO;
import com.ensam.ma.model.Course;
import com.ensam.ma.model.User;
import com.ensam.ma.repository.QuizAttemptRepository;
import com.ensam.ma.service.CourseService;
import com.ensam.ma.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Administrator controller - manages courses, students, and platform
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMINISTRATOR')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final CourseService courseService;
    private final UserService userService;
    private final QuizAttemptRepository quizAttemptRepository;
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("coursesCount", courseService.findAll().size());
        model.addAttribute("publishedCount", courseService.countPublished());
        model.addAttribute("indexedCount", courseService.countIndexed());
        model.addAttribute("studentsCount", userService.countStudents());
        model.addAttribute("quizzesCount", quizAttemptRepository.count());
        
        return "admin/dashboard";
    }
    
    // Course Management
    
    @GetMapping("/courses")
    public String listCourses(Model model) {
        List<Course> courses = courseService.findAll();
        model.addAttribute("courses", courses);
        return "admin/courses/list";
    }
    
    @GetMapping("/courses/new")
    public String newCourseForm(Model model) {
        model.addAttribute("courseDTO", new CourseDTO());
        model.addAttribute("isNew", true);
        return "admin/courses/form";
    }
    
    @PostMapping("/courses/new")
    public String createCourse(@Valid @ModelAttribute("courseDTO") CourseDTO courseDTO,
                               BindingResult result,
                               @AuthenticationPrincipal User currentUser,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("isNew", true);
            return "admin/courses/form";
        }
        
        try {
            Course course = courseService.createCourse(courseDTO, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Course '" + course.getTitle() + "' created successfully!");
            return "redirect:/admin/courses";
        } catch (Exception e) {
            log.error("Error creating course", e);
            model.addAttribute("errorMessage", "Error creating course: " + e.getMessage());
            model.addAttribute("isNew", true);
            return "admin/courses/form";
        }
    }
    
    @GetMapping("/courses/{id}/edit")
    public String editCourseForm(@PathVariable Long id, Model model) {
        Course course = courseService.findById(id);
        
        CourseDTO courseDTO = new CourseDTO();
        courseDTO.setTitle(course.getTitle());
        courseDTO.setDescription(course.getDescription());
        courseDTO.setContent(course.getContent());
        
        model.addAttribute("courseDTO", courseDTO);
        model.addAttribute("courseId", id);
        model.addAttribute("isNew", false);
        return "admin/courses/form";
    }
    
    @PostMapping("/courses/{id}/edit")
    public String updateCourse(@PathVariable Long id,
                               @Valid @ModelAttribute("courseDTO") CourseDTO courseDTO,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("courseId", id);
            model.addAttribute("isNew", false);
            return "admin/courses/form";
        }
        
        try {
            Course course = courseService.updateCourse(id, courseDTO);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Course '" + course.getTitle() + "' updated successfully!");
            return "redirect:/admin/courses";
        } catch (Exception e) {
            log.error("Error updating course", e);
            model.addAttribute("errorMessage", "Error updating course: " + e.getMessage());
            model.addAttribute("courseId", id);
            model.addAttribute("isNew", false);
            return "admin/courses/form";
        }
    }
    
    @PostMapping("/courses/{id}/publish")
    public String publishCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Course course = courseService.publishCourse(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Course '" + course.getTitle() + "' published successfully!");
        } catch (Exception e) {
            log.error("Error publishing course", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error publishing course: " + e.getMessage());
        }
        return "redirect:/admin/courses";
    }
    
    @PostMapping("/courses/{id}/unpublish")
    public String unpublishCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Course course = courseService.unpublishCourse(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Course '" + course.getTitle() + "' unpublished successfully!");
        } catch (Exception e) {
            log.error("Error unpublishing course", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error unpublishing course: " + e.getMessage());
        }
        return "redirect:/admin/courses";
    }
    
    @PostMapping("/courses/{id}/index")
    public String indexCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            courseService.markAsIndexed(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Course indexed successfully for AI quiz generation!");
        } catch (Exception e) {
            log.error("Error indexing course", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error indexing course: " + e.getMessage());
        }
        return "redirect:/admin/courses";
    }
    
    @PostMapping("/courses/{id}/delete")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            courseService.deleteCourse(id);
            redirectAttributes.addFlashAttribute("successMessage", "Course deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting course", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting course: " + e.getMessage());
        }
        return "redirect:/admin/courses";
    }
    
    @GetMapping("/courses/{id}/students")
    public String manageCourseStudents(@PathVariable Long id, Model model) {
        Course course = courseService.findById(id);
        List<User> allStudents = userService.findAllStudents();
        
        model.addAttribute("course", course);
        model.addAttribute("allStudents", allStudents);
        return "admin/courses/students";
    }
    
    @PostMapping("/courses/{courseId}/students/{studentId}/enroll")
    public String enrollStudent(@PathVariable Long courseId, 
                                @PathVariable Long studentId,
                                RedirectAttributes redirectAttributes) {
        try {
            courseService.enrollStudent(courseId, studentId);
            redirectAttributes.addFlashAttribute("successMessage", "Student enrolled successfully!");
        } catch (Exception e) {
            log.error("Error enrolling student", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error enrolling student: " + e.getMessage());
        }
        return "redirect:/admin/courses/" + courseId + "/students";
    }
    
    @PostMapping("/courses/{courseId}/students/{studentId}/unenroll")
    public String unenrollStudent(@PathVariable Long courseId, 
                                  @PathVariable Long studentId,
                                  RedirectAttributes redirectAttributes) {
        try {
            courseService.unenrollStudent(courseId, studentId);
            redirectAttributes.addFlashAttribute("successMessage", "Student unenrolled successfully!");
        } catch (Exception e) {
            log.error("Error unenrolling student", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error unenrolling student: " + e.getMessage());
        }
        return "redirect:/admin/courses/" + courseId + "/students";
    }
    
    // Student Management
    
    @GetMapping("/students")
    public String listStudents(Model model) {
        List<User> students = userService.findAllStudents();
        model.addAttribute("students", students);
        return "admin/students/list";
    }
    
    @GetMapping("/students/new")
    public String newStudentForm(Model model) {
        model.addAttribute("userDTO", new UserDTO());
        model.addAttribute("isNew", true);
        return "admin/students/form";
    }
    
    @PostMapping("/students/new")
    public String createStudent(@Valid @ModelAttribute("userDTO") UserDTO userDTO,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("isNew", true);
            return "admin/students/form";
        }
        
        try {
            User student = userService.createStudent(userDTO);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Student '" + student.getUsername() + "' created successfully!");
            return "redirect:/admin/students";
        } catch (Exception e) {
            log.error("Error creating student", e);
            model.addAttribute("errorMessage", "Error creating student: " + e.getMessage());
            model.addAttribute("isNew", true);
            return "admin/students/form";
        }
    }
    
    @PostMapping("/students/{id}/delete")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Student deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting student", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting student: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }
}
