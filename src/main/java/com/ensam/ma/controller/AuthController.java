package com.ensam.ma.controller;

import com.ensam.ma.dto.RegistrationDTO;
import com.ensam.ma.model.Role;
import com.ensam.ma.model.User;
import com.ensam.ma.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Authentication Controller - Handles student registration
 * Students can self-register via public signup page
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Show student registration form
     */
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationDTO", new RegistrationDTO());
        return "register";
    }

    /**
     * Process student registration
     */
    @PostMapping("/register")
    public String registerStudent(@Valid @ModelAttribute("registrationDTO") RegistrationDTO dto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            return "register";
        }

        // Check if username already exists
        if (userRepository.existsByUsername(dto.getUsername())) {
            bindingResult.rejectValue("username", "error.username", "Username already taken");
            return "register";
        }

        // Check if email already exists
        if (userRepository.existsByEmail(dto.getEmail())) {
            bindingResult.rejectValue("email", "error.email", "Email already registered");
            return "register";
        }

        // Check password confirmation
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Passwords do not match");
            return "register";
        }

        try {
            // Create new student user
            User student = User.builder()
                    .username(dto.getUsername())
                    .password(passwordEncoder.encode(dto.getPassword()))
                    .fullName(dto.getFullName())
                    .email(dto.getEmail())
                    .role(Role.STUDENT)
                    .enabled(true)
                    .build();

            userRepository.save(student);

            log.info("New student registered: {}", student.getUsername());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration successful! You can now login with your credentials.");
            return "redirect:/login";

        } catch (Exception e) {
            log.error("Error during student registration", e);
            model.addAttribute("errorMessage", "Registration failed. Please try again.");
            return "register";
        }
    }
}
