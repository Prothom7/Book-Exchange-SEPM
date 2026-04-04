package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.dto.RegisterRequest;
import com.example.book_exchange_sepm.exception.DuplicateUserException;
import com.example.book_exchange_sepm.exception.ResourceNotFoundException;
import com.example.book_exchange_sepm.exception.UnauthorizedActionException;
import com.example.book_exchange_sepm.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class AuthPageController {

    private final AuthenticationService authenticationService;

    public AuthPageController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "registered", required = false) String registered,
                        @RequestParam(value = "resent", required = false) String resent,
                        @RequestParam(value = "resendError", required = false) String resendError,
                        @RequestParam(value = "resetRequested", required = false) String resetRequested,
                        @RequestParam(value = "resetSuccess", required = false) String resetSuccess,
                        Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Registration successful. Please login.");
        }
        if (resent != null) {
            model.addAttribute("successMessage", "Verification email resent. Please check your inbox.");
        }
        if (resendError != null) {
            model.addAttribute("errorMessage", resendError);
        }
        if (resetRequested != null) {
            model.addAttribute("successMessage", "If the account exists, a password reset link has been sent.");
        }
        if (resetSuccess != null) {
            model.addAttribute("successMessage", "Password reset successful. Please login with your new password.");
        }
        return "login";
    }

    @PostMapping("/login-failure")
    public String loginFailure(Model model) {
        model.addAttribute("errorMessage", "Invalid username or password.");
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            authenticationService.register(registerRequest);
            return "redirect:/login?registered=true";
        } catch (DuplicateUserException | IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "register";
        }
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam(value = "token", required = false) String token,
                              @RequestParam(value = "toker", required = false) String toker,
                              Model model) {
        String effectiveToken = token != null && !token.isBlank() ? token : toker;

        if (effectiveToken == null || effectiveToken.isBlank()) {
            model.addAttribute("verificationSuccess", false);
            model.addAttribute("message", "Verification token is missing. Please use resend verification email.");
            return "verify-email";
        }

        try {
            authenticationService.verifyEmail(effectiveToken);
            model.addAttribute("verificationSuccess", true);
            model.addAttribute("message", "Email verified successfully. You can now login.");
        } catch (ResourceNotFoundException | UnauthorizedActionException ex) {
            model.addAttribute("verificationSuccess", false);
            model.addAttribute("message", ex.getMessage());
        }
        return "verify-email";
    }

    @PostMapping("/resend-verification")
    public String resendVerification(@RequestParam("usernameOrEmail") String usernameOrEmail) {
        try {
            authenticationService.resendVerificationEmail(usernameOrEmail);
            return "redirect:/login?resent=true";
        } catch (ResourceNotFoundException | UnauthorizedActionException ex) {
            String encodedMessage = URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/login?resendError=" + encodedMessage;
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", error);
        }
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestPasswordReset(@RequestParam("usernameOrEmail") String usernameOrEmail) {
        authenticationService.requestPasswordReset(usernameOrEmail);
        return "redirect:/login?resetRequested=true";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(value = "token", required = false) String token,
                                    @RequestParam(value = "error", required = false) String error,
                                    Model model) {
        model.addAttribute("token", token);
        if (error != null) {
            model.addAttribute("errorMessage", error);
        }
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("token") String token,
                                @RequestParam("password") String password,
                                @RequestParam("confirmPassword") String confirmPassword) {
        try {
            authenticationService.resetPassword(token, password, confirmPassword);
            return "redirect:/login?resetSuccess=true";
        } catch (ResourceNotFoundException | UnauthorizedActionException | IllegalArgumentException ex) {
            String encodedMessage = URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/reset-password?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&error=" + encodedMessage;
        }
    }
}
