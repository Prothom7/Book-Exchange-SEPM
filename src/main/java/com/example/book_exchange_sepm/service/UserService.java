package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.UserResponse;
import com.example.book_exchange_sepm.entity.Role;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.exception.ResourceNotFoundException;
import com.example.book_exchange_sepm.exception.UnauthorizedActionException;
import com.example.book_exchange_sepm.repository.BookRepository;
import com.example.book_exchange_sepm.repository.RoleRepository;
import com.example.book_exchange_sepm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RoleRepository roleRepository;

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
     * Check if current user is delivery man
     */
    @Transactional(readOnly = true)
    public boolean isDeliveryMan() {
        User currentUser = getCurrentUserEntity();
        return currentUser.getRoles().stream()
            .anyMatch(role -> role.getName().equals("ROLE_DELIVERY_MAN"));
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
     * Get all users who have the delivery man role.
     */
    @Transactional(readOnly = true)
    public java.util.List<User> getDeliveryMen() {
        return userRepository.findByRoles_Name("ROLE_DELIVERY_MAN");
    }

    /**
     * Get delivery men who were explicitly approved through the request flow.
     * Seeded/demo delivery accounts remain visible in the system but are not
     * eligible for automatic assignment.
     */
    @Transactional(readOnly = true)
    public java.util.List<User> getApprovedDeliveryMenForAssignment() {
        return userRepository.findByRoles_Name("ROLE_DELIVERY_MAN").stream()
            .filter(user -> resolveDeliveryRequestStatus(user) == User.DeliveryRequestStatus.APPROVED)
            .toList();
    }

    /**
     * Request delivery man approval for the current user.
     */
    @Transactional
    public UserResponse requestDeliveryManRole() {
        User currentUser = getCurrentUserEntity();

        boolean alreadyDeliveryMan = currentUser.getRoles().stream()
            .anyMatch(role -> role.getName().equals("ROLE_DELIVERY_MAN"));
        if (alreadyDeliveryMan) {
            throw new UnauthorizedActionException("You are already approved as a delivery man");
        }

        if (resolveDeliveryRequestStatus(currentUser) == User.DeliveryRequestStatus.PENDING) {
            throw new UnauthorizedActionException("Your delivery man request is already pending approval");
        }

        currentUser.setDeliveryRequestStatus(User.DeliveryRequestStatus.PENDING);
        currentUser.setDeliveryRequestRequestedAt(LocalDateTime.now());
        currentUser.setDeliveryRequestApprovedAt(null);

        return convertToUserResponse(userRepository.save(currentUser));
    }

    /**
     * Approve a pending delivery man request.
     */
    @Transactional
    public UserResponse approveDeliveryManRole(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (resolveDeliveryRequestStatus(user) != User.DeliveryRequestStatus.PENDING) {
            throw new UnauthorizedActionException("User does not have a pending delivery request");
        }

        Role deliveryRole = roleRepository.findByName("ROLE_DELIVERY_MAN")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_DELIVERY_MAN")));

        user.getRoles().add(deliveryRole);
        user.setDeliveryRequestStatus(User.DeliveryRequestStatus.APPROVED);
        user.setDeliveryRequestApprovedAt(LocalDateTime.now());

        return convertToUserResponse(userRepository.save(user));
    }

    /**
     * Update current authenticated user's profile image.
     */
    @Transactional
    public UserResponse updateCurrentUserProfileImage(String imageDataUrl) {
        if (imageDataUrl == null || imageDataUrl.isBlank()) {
            throw new UnauthorizedActionException("Profile image data cannot be empty");
        }

        if (!imageDataUrl.startsWith("data:image/")) {
            throw new UnauthorizedActionException("Only image files are supported");
        }

        int commaIndex = imageDataUrl.indexOf(',');
        if (commaIndex <= 0 || commaIndex == imageDataUrl.length() - 1) {
            throw new UnauthorizedActionException("Invalid image format");
        }

        String base64Payload = imageDataUrl.substring(commaIndex + 1);
        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(base64Payload);
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedActionException("Invalid image encoding");
        }

        int maxBytes = 2 * 1024 * 1024; // 2 MB
        if (imageBytes.length > maxBytes) {
            throw new UnauthorizedActionException("Profile image size must be less than 2 MB");
        }

        User currentUser = getCurrentUserEntity();
        currentUser.setProfileImageDataUrl(imageDataUrl);
        User savedUser = userRepository.save(currentUser);
        return convertToUserResponse(savedUser);
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
            user.getUpdatedAt(),
            user.getProfileImageDataUrl(),
            resolveDeliveryRequestStatus(user).name(),
            user.getDeliveryRequestRequestedAt(),
            user.getDeliveryRequestApprovedAt()
        );
    }

    private User.DeliveryRequestStatus resolveDeliveryRequestStatus(User user) {
        return user.getDeliveryRequestStatus() != null
            ? user.getDeliveryRequestStatus()
            : User.DeliveryRequestStatus.NONE;
    }

    /**
     * Promote user to moderator based on book quota
     * Minimum requirement: User must have at least 5 books listed
     */
    public UserResponse promoteToModerator(Long userId, Integer bookQuota) {
        if (bookQuota == null) {
            bookQuota = 5; // Default: 5 books minimum
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Check if user already has moderator role
        boolean isModerator = user.getRoles().stream()
            .anyMatch(role -> role.getName().equals("ROLE_MODERATOR"));
        
        if (isModerator) {
            throw new RuntimeException("User is already a moderator");
        }

        // Count books owned by user
        Long bookCount = (long) bookRepository.findByOwner(user).size();
        
        if (bookCount < bookQuota) {
            throw new RuntimeException(
                String.format(
                    "User does not meet quota. Books: %d, Required: %d",
                    bookCount, bookQuota
                )
            );
        }

        // Get or create ROLE_MODERATOR
        Role moderatorRole = roleRepository.findByName("ROLE_MODERATOR")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_MODERATOR")));

        // Add moderator role to user
        user.getRoles().add(moderatorRole);
        User updatedUser = userRepository.save(user);

        return convertToUserResponse(updatedUser);
    }
}
