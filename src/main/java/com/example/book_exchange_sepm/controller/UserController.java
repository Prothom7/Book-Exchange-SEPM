package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.dto.UserResponse;
import com.example.book_exchange_sepm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<UserResponse> getCurrentUserProfile() {
        UserResponse user = userService.getCurrentUser();
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    /**
     * User dashboard
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<String> userDashboard() {
        return new ResponseEntity<>("Welcome to User Dashboard", HttpStatus.OK);
    }
}
