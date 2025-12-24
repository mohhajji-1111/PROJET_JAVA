package com.ensam.ma.controller;

import com.ensam.ma.dto.CourseDTO;
import com.ensam.ma.model.*;
import com.ensam.ma.repository.EnrollmentRepository;
import com.ensam.ma.repository.QuizAttemptRepository;
import com.ensam.ma.service.*;
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
 * Administrator controller - manages courses, students, validation, and
 * messages
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
    private final EnrollmentRepository enrollmentRepository;
    private final AgenticQuizSupervisor agenticQuizSupervisor;
    private final MessageService messageService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User admin, Model model) {
        model.addAttribute("admin", admin);
        model.addAttribute("totalCourses", courseService.findAll().size());
        model.addAttribute("publishedCourses", courseService.countPublished());
        model.addAttribute("indexedCount", courseService.countIndexed());
        model.addAttribute("totalStudents", userService.countStudents());

        long totalAttempts = quizAttemptRepository.count();
        long completedAttempts = quizAttemptRepository.countCompleted();
        Double avgScore = quizAttemptRepository.averageScore();
        model.addAttribute("quizStats", new QuizStats(totalAttempts, completedAttempts, avgScore));

        // Pending validations count
        long pendingValidations = enrollmentRepository.countPendingValidationsForAdmin(admin.getId());
        model.addAttribute("pendingValidations", pendingValidations);

        // Unread messages count
        long unreadMessages = messageService.getUnreadCount(admin.getId());
        model.addAttribute("unreadMessages", unreadMessages);

        return "admin/dashboard";
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class QuizStats {
        private long totalAttempts;
        private long completed;
        private Double averageScore;
    }

    // ==================== COURSE MANAGEMENT ====================

    @GetMapping("/courses")
    public String listCourses(Model model) {
        List<Course> courses = courseService.findAll();
        model.addAttribute("courses", courses);
        return "admin/courses/list";
    }

    @GetMapping("/courses/{id}")
    public String viewCourse(@PathVariable Long id, Model model) {
        Course course = courseService.findById(id);
        model.addAttribute("course", course);
        return "admin/courses/view";
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
                    "Course '" + course.getTitle() + "' created! Enrollment Code: " + course.getEnrollmentCode());
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
                    "Course '" + course.getTitle() + "' published! Share code: " + course.getEnrollmentCode());
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
                    "Course '" + course.getTitle() + "' unpublished!");
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
                    "Course indexed for AI quiz generation!");
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
            redirectAttributes.addFlashAttribute("successMessage", "Course deleted!");
        } catch (Exception e) {
            log.error("Error deleting course", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting course: " + e.getMessage());
        }
        return "redirect:/admin/courses";
    }

    // ==================== STUDENT MANAGEMENT ====================

    @GetMapping("/students")
    public String listStudents(Model model) {
        List<User> students = userService.findAllStudents();
        model.addAttribute("students", students);
        return "admin/students/list";
    }

    @GetMapping("/students/{id}")
    public String viewStudent(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User student = userService.findById(id);
            List<Course> enrolledCourses = courseService.findByEnrolledStudent(id);
            List<QuizAttempt> quizHistory = quizAttemptRepository.findByStudentIdOrderByStartedAtDesc(id);

            // Get enrollments with status
            List<Enrollment> enrollments = enrollmentRepository.findByStudentIdOrderByEnrolledAtDesc(id);

            long totalQuizzes = quizHistory.size();
            long passedQuizzes = quizHistory.stream().filter(QuizAttempt::isPassed).count();
            Double avgScore = quizHistory.stream()
                    .filter(QuizAttempt::isCompleted)
                    .mapToInt(QuizAttempt::getScore)
                    .average()
                    .orElse(0.0);

            model.addAttribute("student", student);
            model.addAttribute("enrolledCourses", enrolledCourses);
            model.addAttribute("enrollments", enrollments);
            model.addAttribute("quizHistory", quizHistory);
            model.addAttribute("totalQuizzes", totalQuizzes);
            model.addAttribute("passedQuizzes", passedQuizzes);
            model.addAttribute("averageScore", avgScore);

            return "admin/students/view";
        } catch (Exception e) {
            log.error("Error loading student details", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Student not found");
            return "redirect:/admin/students";
        }
    }

    // ==================== VALIDATION APPROVAL ====================

    @GetMapping("/validations")
    public String listPendingValidations(@AuthenticationPrincipal User admin, Model model) {
        List<Enrollment> pendingValidations = enrollmentRepository.findPendingValidationsForAdmin(admin.getId());
        model.addAttribute("pendingValidations", pendingValidations);
        return "admin/validations";
    }

    @PostMapping("/validations/{enrollmentId}/approve")
    public String approveValidation(@PathVariable Long enrollmentId,
            @AuthenticationPrincipal User admin,
            RedirectAttributes redirectAttributes) {
        try {
            agenticQuizSupervisor.approveValidation(enrollmentId, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Course validation approved!");
        } catch (Exception e) {
            log.error("Error approving validation", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/admin/validations";
    }

    // ==================== MESSAGING (ADVICE SYSTEM) ====================

    @GetMapping("/messages")
    public String inbox(@AuthenticationPrincipal User admin, Model model) {
        List<Message> messages = messageService.getInbox(admin.getId());
        List<Message> unreplied = messageService.getUnrepliedAdviceRequests(admin.getId());
        model.addAttribute("messages", messages);
        model.addAttribute("unreplied", unreplied);
        return "admin/messages/inbox";
    }

    @GetMapping("/messages/{id}")
    public String viewMessage(@PathVariable Long id, Model model) {
        Message message = messageService.getMessage(id);
        messageService.markAsRead(id);
        model.addAttribute("message", message);
        return "admin/messages/view";
    }

    @PostMapping("/messages/{id}/reply")
    public String replyToMessage(@PathVariable Long id,
            @RequestParam String content,
            @AuthenticationPrincipal User admin,
            RedirectAttributes redirectAttributes) {
        try {
            messageService.reply(id, admin, content);
            redirectAttributes.addFlashAttribute("successMessage", "Reply sent!");
        } catch (Exception e) {
            log.error("Error sending reply", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/admin/messages";
    }

    @PostMapping("/students/{id}/delete")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Student deleted!");
        } catch (Exception e) {
            log.error("Error deleting student", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting student: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }
}
