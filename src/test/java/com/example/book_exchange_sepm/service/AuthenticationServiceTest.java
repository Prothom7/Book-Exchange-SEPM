package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.AuthResponse;
import com.example.book_exchange_sepm.dto.LoginRequest;
import com.example.book_exchange_sepm.dto.RegisterRequest;
import com.example.book_exchange_sepm.entity.Role;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.exception.DuplicateUserException;
import com.example.book_exchange_sepm.exception.ResourceNotFoundException;
import com.example.book_exchange_sepm.repository.RoleRepository;
import com.example.book_exchange_sepm.repository.UserRepository;
import com.example.book_exchange_sepm.security.JwtUtil;
import com.example.book_exchange_sepm.validation.PasswordStrengthValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordStrengthValidator passwordStrengthValidator;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void register_ShouldCreateUserWithDefaultRole() {
        RegisterRequest request = new RegisterRequest("newuser", "newuser@example.com", "Password@123");
        Role userRole = new Role(1L, "ROLE_USER");

        User saved = new User();
        saved.setId(10L);
        saved.setUsername("newuser");
        saved.setEmail("newuser@example.com");
        saved.setPassword("encoded-password");
        saved.setRoles(Set.of(userRole));
        saved.setEmailVerified(false);

        when(passwordStrengthValidator.validate(anyString())).thenReturn(PasswordStrengthValidator.ValidationResult.valid());
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded-password");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(saved);

        AuthResponse response = authenticationService.register(request);

        assertEquals(10L, response.getId());
        assertEquals("newuser", response.getUsername());
        assertTrue(response.getRoles().contains("ROLE_USER"));
        assertTrue(response.getMessage().contains("verify"));
        verify(userRepository).save(any(User.class));
        verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void register_ShouldThrow_WhenUsernameExists() {
        RegisterRequest request = new RegisterRequest("existing", "existing@example.com", "Password@123");
        
        when(passwordStrengthValidator.validate(anyString())).thenReturn(PasswordStrengthValidator.ValidationResult.valid());
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> authenticationService.register(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("demo", "Password@123");
        Role role = new Role(1L, "ROLE_USER");

        User user = new User();
        user.setId(99L);
        user.setUsername("demo");
        user.setEmail("demo@example.com");
        user.setRoles(Set.of(role));
        user.setEmailVerified(true);

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
            "demo", "Password@123", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken("demo", "Password@123");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(userRepository.findByUsernameOrEmail("demo", "demo")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("demo")).thenReturn(userDetails);
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        AuthResponse response = authenticationService.login(request);

        assertEquals(99L, response.getId());
        assertEquals("demo", response.getUsername());
        assertTrue(response.getRoles().contains("ROLE_USER"));
        assertEquals("Login successful", response.getMessage());
        assertEquals("jwt-token", response.getToken());
    }

    @Test
    void login_ShouldThrowResourceNotFound_WhenAuthenticationFails() {
        LoginRequest request = new LoginRequest("demo", "wrong-password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        ResourceNotFoundException ex =
            assertThrows(ResourceNotFoundException.class, () -> authenticationService.login(request));

        assertEquals("Invalid username or password", ex.getMessage());
    }
}
