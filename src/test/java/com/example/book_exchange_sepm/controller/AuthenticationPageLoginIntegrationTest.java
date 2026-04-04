package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.entity.Role;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.repository.BookRepository;
import com.example.book_exchange_sepm.repository.ChatMessageRepository;
import com.example.book_exchange_sepm.repository.ChatRoomRepository;
import com.example.book_exchange_sepm.repository.ExchangeRequestRepository;
import com.example.book_exchange_sepm.repository.RoleRepository;
import com.example.book_exchange_sepm.repository.UserNotificationRepository;
import com.example.book_exchange_sepm.repository.UserRepository;
import com.example.book_exchange_sepm.repository.WishlistSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationPageLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private WishlistSubscriptionRepository wishlistSubscriptionRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        exchangeRequestRepository.deleteAll();
        userNotificationRepository.deleteAll();
        wishlistSubscriptionRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void browserFormLogin_ShouldAllowUnverifiedUser_WhenCredentialsAreValid() throws Exception {
        Role userRole = ensureRole("ROLE_USER");
        saveUser("browser_user", "browser@example.com", "BrowserPass@123", false, userRole);

        mockMvc.perform(formLogin("/login").user("browser_user").password("BrowserPass@123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/landingpage"));
    }

    @Test
    void browserFormLogin_ShouldAllowVerifiedUser_WhenCredentialsAreValid() throws Exception {
        Role userRole = ensureRole("ROLE_USER");
        saveUser("verified_browser", "verified.browser@example.com", "VerifiedPass@123", true, userRole);

        mockMvc.perform(formLogin("/login").user("verified_browser").password("VerifiedPass@123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/landingpage"));
    }

    private Role ensureRole(String roleName) {
        return roleRepository.findByName(roleName)
            .orElseGet(() -> roleRepository.save(new Role(null, roleName)));
    }

    private User saveUser(String username,
                          String email,
                          String rawPassword,
                          boolean emailVerified,
                          Role... roles) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmailVerified(emailVerified);
        user.setRoles(Set.of(roles));
        return userRepository.save(user);
    }
}
