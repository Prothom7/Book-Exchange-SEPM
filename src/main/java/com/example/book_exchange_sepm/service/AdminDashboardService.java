package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.entity.*;
import com.example.book_exchange_sepm.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Dashboard Service
 * Provides comprehensive view of all system data for moderators/admins
 * Integrates with multiple repositories to gather system-wide information
 */
@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ExchangeRequestRepository exchangeRequestRepository;
    private final WishlistSubscriptionRepository wishlistSubscriptionRepository;
    private final UserNotificationRepository userNotificationRepository;

    public AdminDashboardService(
            UserRepository userRepository,
            BookRepository bookRepository,
            ExchangeRequestRepository exchangeRequestRepository,
            WishlistSubscriptionRepository wishlistSubscriptionRepository,
            UserNotificationRepository userNotificationRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.exchangeRequestRepository = exchangeRequestRepository;
        this.wishlistSubscriptionRepository = wishlistSubscriptionRepository;
        this.userNotificationRepository = userNotificationRepository;
    }

    /**
     * Get complete dashboard statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalUsers", userRepository.count());
        stats.put("totalBooks", bookRepository.count());
        stats.put("availableBooks", bookRepository.findByAvailableTrue().size());
        stats.put("totalExchanges", exchangeRequestRepository.count());
        stats.put("pendingExchanges", exchangeRequestRepository.findByStatusOrderByCreatedAtDesc(ExchangeRequest.Status.PENDING).size());
        stats.put("approvedRequests", exchangeRequestRepository.findByStatusOrderByCreatedAtDesc(ExchangeRequest.Status.APPROVED).size());
        stats.put("totalWishlistSubscriptions", wishlistSubscriptionRepository.count());
        stats.put("activeWishlists", wishlistSubscriptionRepository.count()); // TODO: Add method for active count
        stats.put("totalNotifications", userNotificationRepository.count());
        stats.put("unreadNotifications", userNotificationRepository.count()); // TODO: Add method for unread count
        
        return stats;
    }

    /**
     * Get all users with their statistics
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllUsersWithStats() {
        return userRepository.findAll().stream()
                .map(user -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("name", user.getUsername()); // Use username as name since User doesn't have name field
                    userInfo.put("phone", "");  // No phone field in User entity
                    userInfo.put("email", user.getEmail());
                    userInfo.put("isEmailVerified", user.getEmailVerified());
                    // Get the primary role (highest priority)
                    String role = user.getRoles().stream()
                        .map(Role::getName)
                        .sorted((a, b) -> {
                            // Sort by role priority: ADMIN > MODERATOR > USER
                            int priorityA = a.contains("ADMIN") ? 0 : a.contains("MODERATOR") ? 1 : 2;
                            int priorityB = b.contains("ADMIN") ? 0 : b.contains("MODERATOR") ? 1 : 2;
                            return Integer.compare(priorityA, priorityB);
                        })
                        .findFirst()
                        .orElse("USER")
                        .replace("ROLE_", "");
                    userInfo.put("role", role);
                    userInfo.put("status", "ACTIVE"); // Default to ACTIVE
                    userInfo.put("bookCount", bookRepository.findByOwner(user).size());
                    userInfo.put("exchangeCount", exchangeRequestRepository.findByRequester_IdOrderByCreatedAtDesc(user.getId()).size());
                    userInfo.put("joinDate", user.getCreatedAt());
                    userInfo.put("lastLogin", user.getUpdatedAt());
                    return userInfo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all books with owner and status information
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllBooksWithDetails() {
        return bookRepository.findAll().stream()
                .map(book -> {
                    Map<String, Object> bookInfo = new HashMap<>();
                    bookInfo.put("id", book.getId());
                    bookInfo.put("title", book.getTitle());
                    bookInfo.put("author", book.getAuthor());
                    bookInfo.put("genre", book.getGenre());
                    bookInfo.put("condition", book.getBookCondition());
                    bookInfo.put("status", book.getAvailable() ? "AVAILABLE" : "ALLOCATED");
                    bookInfo.put("language", book.getLanguage());
                    bookInfo.put("isbn", book.getIsbn());
                    bookInfo.put("owner", book.getOwner().getUsername());
                    bookInfo.put("ownerId", book.getOwner().getId());
                    bookInfo.put("description", book.getDescription());
                    bookInfo.put("createdAt", book.getCreatedAt());
                    return bookInfo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all exchange requests with related information
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllExchangeRequests() {
        return exchangeRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(request -> {
                    Map<String, Object> requestInfo = new HashMap<>();
                    requestInfo.put("id", request.getId());
                    // Sender is the one requesting the exchange (the requester)
                    requestInfo.put("senderName", request.getRequester().getUsername());
                    requestInfo.put("senderId", request.getRequester().getId());
                    // Receiver is the book owner
                    requestInfo.put("receiverName", request.getOwner().getUsername());
                    requestInfo.put("receiverId", request.getOwner().getId());
                    // Books being exchanged
                    requestInfo.put("receiverBook", request.getBook().getTitle());  // Book the requester wants
                    requestInfo.put("senderBook", request.getOfferedBook() != null ? 
                        request.getOfferedBook().getTitle() : "Book offered");  // Book the requester offers
                    requestInfo.put("status", request.getStatus().toString());
                    requestInfo.put("notes", request.getMessage());
                    requestInfo.put("moderatorComment", request.getModeratorComment());
                    requestInfo.put("reviewedBy", request.getReviewedBy() != null ? request.getReviewedBy().getUsername() : null);
                    requestInfo.put("reviewedAt", request.getReviewedAt());
                    requestInfo.put("createdAt", request.getCreatedAt());
                    return requestInfo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all pending exchange requests (for quick access)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingExchangeRequests() {
        return exchangeRequestRepository.findByStatusOrderByCreatedAtDesc(ExchangeRequest.Status.PENDING).stream()
                .map(request -> {
                    Map<String, Object> requestInfo = new HashMap<>();
                    requestInfo.put("id", request.getId());
                    requestInfo.put("senderName", request.getRequester().getUsername());
                    requestInfo.put("senderId", request.getRequester().getId());
                    requestInfo.put("receiverName", request.getOwner().getUsername());
                    requestInfo.put("receiverId", request.getOwner().getId());
                    requestInfo.put("receiverBook", request.getBook().getTitle());
                    requestInfo.put("senderBook", request.getOfferedBook() != null ? request.getOfferedBook().getTitle() : "N/A");
                    requestInfo.put("notes", request.getMessage());
                    requestInfo.put("status", request.getStatus().toString());
                    requestInfo.put("createdAt", request.getCreatedAt());
                    return requestInfo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all wishlist subscriptions with subscription details
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllWishlistSubscriptions() {
        return wishlistSubscriptionRepository.findAll().stream()
                .map(subscription -> {
                    Map<String, Object> wishInfo = new HashMap<>();
                    wishInfo.put("id", subscription.getId());
                    wishInfo.put("userName", subscription.getUser().getUsername());
                    wishInfo.put("userId", subscription.getUser().getId());
                    wishInfo.put("bookTitle", subscription.getBookTitle());
                    wishInfo.put("author", subscription.getAuthor());
                    wishInfo.put("genre", subscription.getGenre());
                    wishInfo.put("active", subscription.getActive());
                    wishInfo.put("createdAt", subscription.getCreatedAt());
                    return wishInfo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get system activity summary
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getActivitySummary() {
        Map<String, Object> activity = new HashMap<>();
        
        long approvedThisMonth = exchangeRequestRepository.findByStatusOrderByCreatedAtDesc(ExchangeRequest.Status.APPROVED).stream()
                .filter(r -> r.getReviewedAt() != null)
                .count();
        
        activity.put("approvedThisMonth", approvedThisMonth);
        activity.put("pendingApproval", getPendingExchangeRequests().size());
        activity.put("activeWishlists", wishlistSubscriptionRepository.findAll().stream()
                .filter(WishlistSubscription::getActive)
                .count());
        activity.put("recentNotifications", userNotificationRepository.count());
        
        return activity;
    }
}
