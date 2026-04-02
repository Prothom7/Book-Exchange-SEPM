package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.AuthResponse;
import com.example.book_exchange_sepm.dto.RegisterRequest;
import com.example.book_exchange_sepm.entity.Role;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.repository.BookRepository;
import com.example.book_exchange_sepm.repository.ExchangeRequestRepository;
import com.example.book_exchange_sepm.repository.RoleRepository;
import com.example.book_exchange_sepm.repository.UserNotificationRepository;
import com.example.book_exchange_sepm.repository.UserRepository;
import com.example.book_exchange_sepm.repository.WishlistSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class AuthenticationLifecycleIntegrationTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @Autowired
    private WishlistSubscriptionRepository wishlistSubscriptionRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        exchangeRequestRepository.deleteAll();
        userNotificationRepository.deleteAll();
        wishlistSubscriptionRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();

        roleRepository.findByName("ROLE_USER")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_USER")));
    }

    @Test
    void verifyEmail_ShouldMarkUserVerified_AndClearVerificationFields() {
        AuthResponse response = authenticationService.register(
            new RegisterRequest("verify_user", "verify@example.com", "VerifyPass@123")
        );

        User registeredUser = userRepository.findById(response.getId()).orElseThrow();
        String token = registeredUser.getVerificationToken();

        AuthResponse verified = authenticationService.verifyEmail(token);
        User updatedUser = userRepository.findById(response.getId()).orElseThrow();

        assertTrue(verified.getEmailVerified());
        assertTrue(updatedUser.getEmailVerified());
        assertNull(updatedUser.getVerificationToken());
        assertNull(updatedUser.getVerificationTokenExpiry());
    }

    @Test
    void resendVerificationEmail_ShouldRotateToken_AndExtendExpiry() {
        AuthResponse response = authenticationService.register(
            new RegisterRequest("resend_user", "resend@example.com", "ResendPass@123")
        );

        User beforeResend = userRepository.findById(response.getId()).orElseThrow();
        String oldToken = beforeResend.getVerificationToken();
        LocalDateTime oldExpiry = beforeResend.getVerificationTokenExpiry();

        authenticationService.resendVerificationEmail("resend_user");

        User afterResend = userRepository.findById(response.getId()).orElseThrow();

        assertFalse(afterResend.getEmailVerified());
        assertNotNull(afterResend.getVerificationToken());
        assertNotEquals(oldToken, afterResend.getVerificationToken());
        assertNotNull(afterResend.getVerificationTokenExpiry());
        assertTrue(afterResend.getVerificationTokenExpiry().isAfter(oldExpiry.minusSeconds(1)));
        assertEquals("resend@example.com", afterResend.getEmail());
    }
}
