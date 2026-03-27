package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.dto.AuthResponse;
import com.example.book_exchange_sepm.dto.LoginRequest;
import com.example.book_exchange_sepm.dto.RegisterRequest;
import com.example.book_exchange_sepm.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authenticationService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authenticationService.login(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestParam(value = "token", required = false) String token,
                                                    @RequestParam(value = "toker", required = false) String toker) {
        String effectiveToken = token != null && !token.isBlank() ? token : toker;
        AuthResponse response = authenticationService.verifyEmail(effectiveToken);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
