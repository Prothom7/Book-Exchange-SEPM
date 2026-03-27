package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.UserResponse;
import com.example.book_exchange_sepm.entity.Book;
import com.example.book_exchange_sepm.entity.Role;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.exception.ResourceNotFoundException;
import com.example.book_exchange_sepm.exception.UnauthorizedActionException;
import com.example.book_exchange_sepm.repository.BookRepository;
import com.example.book_exchange_sepm.repository.RoleRepository;
import com.example.book_exchange_sepm.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserById_ShouldReturnUserResponse_WhenUserExists() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setRoles(Set.of(new Role(1L, "ROLE_USER")));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1L);

        assertEquals("alice", response.getUsername());
        assertTrue(response.getRoles().contains("ROLE_USER"));
    }

    @Test
    void getUserById_ShouldThrow_WhenUserMissing() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(999L));
    }

    @Test
    void updateCurrentUserProfileImage_ShouldThrow_WhenImageFormatInvalid() {
        assertThrows(UnauthorizedActionException.class,
            () -> userService.updateCurrentUserProfileImage("not-a-data-url"));
    }

    @Test
    void updateCurrentUserProfileImage_ShouldThrow_WhenImageTooLarge() {
        setCurrentAuth("maria");

        User user = new User();
        user.setId(3L);
        user.setUsername("maria");
        user.setEmail("maria@example.com");

        when(userRepository.findByUsername("maria")).thenReturn(Optional.of(user));

        byte[] large = new byte[(2 * 1024 * 1024) + 5];
        String payload = Base64.getEncoder().encodeToString(large);
        String dataUrl = "data:image/png;base64," + payload;

        assertThrows(UnauthorizedActionException.class,
            () -> userService.updateCurrentUserProfileImage(dataUrl));
    }

    @Test
    void promoteToModerator_ShouldAddModeratorRole_WhenQuotaMet() {
        User user = new User();
        user.setId(4L);
        user.setUsername("reader1");
        user.setEmail("reader1@example.com");
        user.setRoles(new HashSet<>(Set.of(new Role(1L, "ROLE_USER"))));

        when(userRepository.findById(4L)).thenReturn(Optional.of(user));
        when(bookRepository.findByOwner(user)).thenReturn(List.of(new Book(), new Book(), new Book(), new Book(), new Book()));
        when(roleRepository.findByName("ROLE_MODERATOR")).thenReturn(Optional.of(new Role(2L, "ROLE_MODERATOR")));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.promoteToModerator(4L, 5);

        assertTrue(response.getRoles().contains("ROLE_MODERATOR"));
        verify(userRepository).save(user);
    }

    @Test
    void promoteToModerator_ShouldThrow_WhenQuotaNotMet() {
        User user = new User();
        user.setId(5L);
        user.setRoles(new HashSet<>(Set.of(new Role(1L, "ROLE_USER"))));

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(bookRepository.findByOwner(user)).thenReturn(List.of(new Book(), new Book()));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> userService.promoteToModerator(5L, 5));

        assertTrue(ex.getMessage().contains("does not meet quota"));
    }

    private void setCurrentAuth(String username) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(username, "n/a")
        );
    }
}
