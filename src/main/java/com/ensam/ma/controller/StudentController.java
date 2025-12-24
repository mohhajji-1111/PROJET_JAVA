package com.ensam.ma.controller;

import com.ensam.ma.dto.QuizSubmissionWrapper;
import com.ensam.ma.model.Course;
import com.ensam.ma.model.QuizAttempt;
import com.ensam.ma.model.User;
import com.ensam.ma.service.AgenticAIService;
import com.ensam.ma.service.CourseService;
import com.ensam.ma.service.MessageService;
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
 * Student controller - handles student course access, quiz taking, and teacher
 * communication
 */
@Controller
@RequestMapping("/student")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
@Slf4j
public class StudentController {

    private final CourseService courseService;
    private final QuizService quizService;
    private final MessageService messageService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User student, Model model) {
        List<Course> courses = courseService.findPublishedCoursesByStudent(student.getId());
        List<QuizAttempt> recentAttempts = quizService.findRecentAttemptsByStudent(student.getId(), 5);

        model.addAttribute("student", student);
        model.addAttribute("courses", courses);
        model.addAttribute("recentAttempts", recentAttempts);

        return "student/dashboard";
    }

    @GetMapping("/courses")
    public String listCourses(@AuthenticationPrincipal User student, Model model) {
        List<Course> courses = courseService.findPublishedCoursesByStudent(student.getId());
        model.addAttribute("student", student);
        model.addAttribute("courses", courses);
        return "student/courses/list";
    }

    /**
     * Join a course using an enrollment code
     */
    @PostMapping("/courses/join")
    public String joinCourseByCode(@RequestParam("enrollmentCode") String enrollmentCode,
            @AuthenticationPrincipal User student,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Student {} attempting to join with code: {}", student.getUsername(), enrollmentCode);
            Course course = courseService.enrollByCode(enrollmentCode, student);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully enrolled in '" + course.getTitle() + "'!");
            log.info("Successfully enrolled {} in course {}", student.getUsername(), course.getTitle());
            return "redirect:/student/dashboard";
        } catch (Exception e) {
            log.error("Error joining course by code: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/dashboard";
        }
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

    /**
     * Send advice request to course teacher
     */
    @PostMapping("/courses/{id}/ask-advice")
    public String askAdvice(@PathVariable Long id,
            @RequestParam String subject,
            @RequestParam String content,
            @AuthenticationPrincipal User student,
            RedirectAttributes redirectAttributes) {
        try {
            Course course = courseService.findById(id);
            // Use messageService injected via constructor
            messageService.sendAdviceRequest(student, course, subject, content);
            redirectAttributes.addFlashAttribute("successMessage", "Your message has been sent to the teacher!");
        } catch (Exception e) {
            log.error("Error sending advice request", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/student/courses/" + id;
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

            // Create submission wrapper with proper size
            QuizSubmissionWrapper submission = new QuizSubmissionWrapper();
            for (int i = 0; i < attempt.getQuestions().size(); i++) {
                submission.getAnswers().add(""); // Initialize with empty strings
            }

            model.addAttribute("attempt", attempt);
            model.addAttribute("submission", submission);
            return "student/quiz/take";
        } catch (Exception e) {
            log.error("Error loading quiz", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error loading quiz: " + e.getMessage());
            return "redirect:/student/courses";
        }
    }

    @PostMapping("/quiz/{attemptId}/submit")
    public String submitQuiz(@PathVariable Long attemptId,
            @ModelAttribute("submission") QuizSubmissionWrapper submission,
            @AuthenticationPrincipal User student,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Submitting quiz {} with answers: {}", attemptId, submission.getAnswers());

            QuizAttempt attempt = quizService.getQuizAttempt(attemptId);

            // Verify ownership
            if (!attempt.getStudent().getId().equals(student.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized access to quiz");
                return "redirect:/student/courses";
            }

            // Convert string answers to integers
            List<Integer> answers = submission.getAnswersAsIntegers();
            log.info("Converted answers: {}", answers);

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
        List<QuizAttempt> attempts = quizService.getStudentAllQuizzes(student);
        model.addAttribute("attempts", attempts);
        return "student/quiz/history";
    }
}
