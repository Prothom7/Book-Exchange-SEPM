package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.AuthResponse;
import com.example.book_exchange_sepm.dto.LoginRequest;
import com.example.book_exchange_sepm.dto.RegisterRequest;
import com.example.book_exchange_sepm.entity.Role;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.exception.DuplicateUserException;
import com.example.book_exchange_sepm.exception.UnauthorizedActionException;
import com.example.book_exchange_sepm.repository.RoleRepository;
import com.example.book_exchange_sepm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class AuthenticationServiceIntegrationTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void register_ShouldPersistUserWithDefaultRole() {
        RegisterRequest request = new RegisterRequest("test_user", "test@example.com", "TestPass@123");

        AuthResponse response = authenticationService.register(request);

        assertNotNull(response.getId());
        assertEquals("test_user", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertTrue(response.getRoles().contains("ROLE_USER"));
        assertTrue(response.getMessage().contains("verify"));
        assertFalse(response.getEmailVerified());

        assertTrue(userRepository.existsByUsername("test_user"));
        assertTrue(userRepository.existsByEmail("test@example.com"));
    }

    @Test
    void register_ShouldThrowDuplicateUserException_WhenUsernameExists() {
        RegisterRequest request = new RegisterRequest("duplicate", "first@example.com", "DupPass@123");
        authenticationService.register(request);

        RegisterRequest dupRequest = new RegisterRequest("duplicate", "second@example.com", "DupPass@456");

        assertThrows(DuplicateUserException.class, () -> authenticationService.register(dupRequest));
    }

    @Test
    void login_ShouldThrowUnauthorized_WhenEmailNotVerified() {
        RegisterRequest registerRequest = new RegisterRequest("login_test", "login_test@example.com", "SecurePass@123");
        authenticationService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest("login_test", "SecurePass@123");
        
        // Should throw UnauthorizedActionException because email is not verified
        assertThrows(UnauthorizedActionException.class, () -> authenticationService.login(loginRequest));
    }

    @Test
    void login_ShouldSucceed_AfterEmailVerification() {
        // Register user
        RegisterRequest registerRequest = new RegisterRequest("verified_user", "verified@example.com", "VerifiedPass@123");
        authenticationService.register(registerRequest);

        // Manually verify email (simulate clicking verification link)
        User user = userRepository.findByUsername("verified_user").orElseThrow();
        user.setEmailVerified(true);
        userRepository.save(user);

        // Now login should work
        LoginRequest loginRequest = new LoginRequest("verified_user", "VerifiedPass@123");
        AuthResponse response = authenticationService.login(loginRequest);

        assertEquals("verified_user", response.getUsername());
        assertEquals("verified@example.com", response.getEmail());
        assertTrue(response.getRoles().contains("ROLE_USER"));
        assertEquals("Login successful", response.getMessage());
        assertTrue(response.getEmailVerified());
        assertNotNull(response.getToken());
    }
}
