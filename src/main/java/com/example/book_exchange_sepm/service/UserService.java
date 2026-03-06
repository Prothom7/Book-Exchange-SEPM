package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.UserResponse;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.exception.ResourceNotFoundException;
import com.example.book_exchange_sepm.exception.UnauthorizedActionException;
import com.example.book_exchange_sepm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Get user by ID as DTO
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return convertToUserResponse(user);
    }

    /**
     * Get user by username as DTO
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        return convertToUserResponse(user);
    }

    /**
     * Get current authenticated user as DTO
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        return convertToUserResponse(user);
    }

    /**
     * Get current authenticated user's ID
     */
    @Transactional(readOnly = true)
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        return user.getId();
    }

    /**
     * Get current authenticated user as User entity
     */
    @Transactional(readOnly = true)
    public User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    /**
     * Find user by username or throw exception
     */
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    /**
     * Find user by ID or throw exception
     */
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    /**
     * Check if current user owns the resource with given ownerID
     * Admin can bypass (owns all resources)
     */
    @Transactional(readOnly = true)
    public boolean isOwnerOrAdmin(Long ownerId) {
        User currentUser = getCurrentUserEntity();

        // Admin can bypass ownership
        boolean isAdmin = currentUser.getRoles().stream()
            .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return true;
        }

        // Check ownership
        return currentUser.getId().equals(ownerId);
    }

    /**
     * Validate ownership or admin privilege
     * Throws UnauthorizedActionException if not authorized
     */
    @Transactional(readOnly = true)
    public void validateOwnershipOrAdmin(Long ownerId) {
        if (!isOwnerOrAdmin(ownerId)) {
            throw new UnauthorizedActionException("You do not have permission to modify this resource");
        }
    }

    /**
     * Check if current user is admin
     */
    @Transactional(readOnly = true)
    public boolean isAdmin() {
        User currentUser = getCurrentUserEntity();
        return currentUser.getRoles().stream()
            .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));
    }

    /**
     * Check if current user is moderator
     */
    @Transactional(readOnly = true)
    public boolean isModerator() {
        User currentUser = getCurrentUserEntity();
        return currentUser.getRoles().stream()
            .anyMatch(role -> role.getName().equals("ROLE_MODERATOR"));
    }

    /**
     * Check if current user has given role
     */
    @Transactional(readOnly = true)
    public boolean hasRole(String roleName) {
        User currentUser = getCurrentUserEntity();
        return currentUser.getRoles().stream()
            .anyMatch(role -> role.getName().equals(roleName));
    }

    /**
     * Convert User entity to UserResponse DTO
     */
    private UserResponse convertToUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet()),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
