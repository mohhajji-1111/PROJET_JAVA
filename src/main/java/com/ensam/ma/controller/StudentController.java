package com.ensam.ma.controller;

import com.ensam.ma.model.Course;
import com.ensam.ma.model.QuizAttempt;
import com.ensam.ma.model.User;
import com.ensam.ma.service.AgenticAIService;
import com.ensam.ma.service.CourseService;
import com.ensam.ma.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Student controller - handles student course access and quiz taking
 */
@Controller
@RequestMapping("/student")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
@Slf4j
public class StudentController {
    
    private final CourseService courseService;
    private final QuizService quizService;
    
    @GetMapping("/courses")
    public String listCourses(@AuthenticationPrincipal User student, Model model) {
        List<Course> courses = courseService.findPublishedCoursesByStudent(student.getId());
        model.addAttribute("courses", courses);
        return "student/courses/list";
    }
    
    @GetMapping("/courses/{id}")
    public String viewCourse(@PathVariable Long id,
                             @AuthenticationPrincipal User student,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            Course course = courseService.findById(id);
            
            // Verify enrollment
            if (!courseService.isStudentEnrolled(id, student.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "You are not enrolled in this course");
                return "redirect:/student/courses";
            }
            
            // Get quiz history for this course
            List<QuizAttempt> quizHistory = quizService.getStudentQuizHistory(student, course);
            
            model.addAttribute("course", course);
            model.addAttribute("quizHistory", quizHistory);
            return "student/courses/view";
        } catch (Exception e) {
            log.error("Error viewing course", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error loading course: " + e.getMessage());
            return "redirect:/student/courses";
        }
    }
    
    @PostMapping("/courses/{id}/generate-quiz")
    public String generateQuiz(@PathVariable Long id,
                               @AuthenticationPrincipal User student,
                               RedirectAttributes redirectAttributes) {
        try {
            Course course = courseService.findById(id);
            QuizAttempt attempt = quizService.generateQuiz(student, course);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Quiz generated! The AI has created " + attempt.getTotalQuestions() + 
                " questions at " + attempt.getDifficulty() + " difficulty.");
            
            return "redirect:/student/quiz/" + attempt.getId();
        } catch (Exception e) {
            log.error("Error generating quiz", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error generating quiz: " + e.getMessage());
            return "redirect:/student/courses/" + id;
        }
    }
    
    @GetMapping("/quiz/{attemptId}")
    public String takeQuiz(@PathVariable Long attemptId,
                           @AuthenticationPrincipal User student,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            QuizAttempt attempt = quizService.getQuizAttempt(attemptId);
            
            // Verify ownership
            if (!attempt.getStudent().getId().equals(student.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized access to quiz");
                return "redirect:/student/courses";
            }
            
            // If already completed, show results
            if (attempt.isCompleted()) {
                return "redirect:/student/quiz/" + attemptId + "/results";
            }
            
            model.addAttribute("attempt", attempt);
            return "student/quiz/take";
        } catch (Exception e) {
            log.error("Error loading quiz", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error loading quiz: " + e.getMessage());
            return "redirect:/student/courses";
        }
    }
    
    @PostMapping("/quiz/{attemptId}/submit")
    public String submitQuiz(@PathVariable Long attemptId,
                             @RequestParam("answers") List<Integer> answers,
                             @AuthenticationPrincipal User student,
                             RedirectAttributes redirectAttributes) {
        try {
            QuizAttempt attempt = quizService.getQuizAttempt(attemptId);
            
            // Verify ownership
            if (!attempt.getStudent().getId().equals(student.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized access to quiz");
                return "redirect:/student/courses";
            }
            
            AgenticAIService.EvaluationResult evaluation = quizService.submitQuiz(attemptId, answers);
            
            redirectAttributes.addFlashAttribute("evaluationResult", evaluation);
            redirectAttributes.addFlashAttribute("successMessage", "Quiz submitted successfully!");
            
            return "redirect:/student/quiz/" + attemptId + "/results";
        } catch (Exception e) {
            log.error("Error submitting quiz", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error submitting quiz: " + e.getMessage());
            return "redirect:/student/quiz/" + attemptId;
        }
    }
    
    @GetMapping("/quiz/{attemptId}/results")
    public String viewResults(@PathVariable Long attemptId,
                              @AuthenticationPrincipal User student,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            QuizAttempt attempt = quizService.getQuizAttempt(attemptId);
            
            // Verify ownership
            if (!attempt.getStudent().getId().equals(student.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized access to quiz");
                return "redirect:/student/courses";
            }
            
            if (!attempt.isCompleted()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Quiz not yet completed");
                return "redirect:/student/quiz/" + attemptId;
            }
            
            model.addAttribute("attempt", attempt);
            return "student/quiz/results";
        } catch (Exception e) {
            log.error("Error loading results", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error loading results: " + e.getMessage());
            return "redirect:/student/courses";
        }
    }
    
    @GetMapping("/quiz-history")
    public String quizHistory(@AuthenticationPrincipal User student, Model model) {
        List<QuizAttempt> allQuizzes = quizService.getStudentAllQuizzes(student);
        model.addAttribute("quizzes", allQuizzes);
        return "student/quiz/history";
    }
}
