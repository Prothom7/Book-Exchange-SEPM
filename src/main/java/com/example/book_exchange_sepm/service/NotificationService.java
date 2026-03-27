package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.UserNotificationResponse;
import com.example.book_exchange_sepm.entity.Book;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.entity.UserNotification;
import com.example.book_exchange_sepm.entity.WishlistSubscription;
import com.example.book_exchange_sepm.event.BookAvailableEvent;
import com.example.book_exchange_sepm.exception.ResourceNotFoundException;
import com.example.book_exchange_sepm.pattern.singleton.BookAvailabilitySubscriber;
import com.example.book_exchange_sepm.pattern.singleton.BookEventManager;
import com.example.book_exchange_sepm.repository.BookRepository;
import com.example.book_exchange_sepm.repository.UserNotificationRepository;
import com.example.book_exchange_sepm.repository.WishlistSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationService implements BookAvailabilitySubscriber {

    private final UserNotificationRepository userNotificationRepository;
    private final WishlistSubscriptionRepository wishlistSubscriptionRepository;
    private final BookRepository bookRepository;
    private final UserService userService;

    public NotificationService(UserNotificationRepository userNotificationRepository,
                               WishlistSubscriptionRepository wishlistSubscriptionRepository,
                               BookRepository bookRepository,
                               UserService userService) {
        this.userNotificationRepository = userNotificationRepository;
        this.wishlistSubscriptionRepository = wishlistSubscriptionRepository;
        this.bookRepository = bookRepository;
        this.userService = userService;
    }

    @PostConstruct
    public void registerSubscriber() {
        BookEventManager.getInstance().subscribe(this);
    }

    @PreDestroy
    public void unregisterSubscriber() {
        BookEventManager.getInstance().unsubscribe(this);
    }

    @Transactional(readOnly = true)
    public List<UserNotificationResponse> getMyNotifications() {
        Long currentUserId = userService.getCurrentUserId();
        return userNotificationRepository.findByUserIdOrderByCreatedAtDesc(currentUserId).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public UserNotificationResponse markAsRead(Long notificationId) {
        Long currentUserId = userService.getCurrentUserId();
        UserNotification notification = userNotificationRepository.findByIdAndUserId(notificationId, currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notification.setRead(true);
        UserNotification updated = userNotificationRepository.save(notification);
        return convertToResponse(updated);
    }

    @Transactional
    public void onBookAvailable(BookAvailableEvent event) {
        Book availableBook = bookRepository.findById(event.getBookId())
            .orElseThrow(() -> new ResourceNotFoundException("Book not found for notification event"));

        List<WishlistSubscription> activeSubscriptions = wishlistSubscriptionRepository.findByActiveTrue();
        Set<Long> notifiedUserIds = new HashSet<>();

        for (WishlistSubscription subscription : activeSubscriptions) {
            if (!matches(subscription, event)) {
                continue;
            }
            User subscriber = subscription.getUser();
            if (!notifiedUserIds.add(subscriber.getId())) {
                continue;
            }

            UserNotification notification = new UserNotification();
            notification.setUser(subscriber);
            notification.setBook(availableBook);
            notification.setRead(false);
            notification.setMessage(buildMessage(availableBook));
            userNotificationRepository.save(notification);
        }
    }

    private UserNotificationResponse convertToResponse(UserNotification notification) {
        return new UserNotificationResponse(
            notification.getId(),
            notification.getMessage(),
            notification.getBook() != null ? notification.getBook().getId() : null,
            notification.getBook() != null ? notification.getBook().getTitle() : null,
            notification.getRead(),
            notification.getCreatedAt()
        );
    }

    private boolean matches(WishlistSubscription subscription, BookAvailableEvent event) {
        if (!containsIgnoreCase(event.getTitle(), subscription.getBookTitle())) {
            return false;
        }

        String expectedAuthor = subscription.getAuthor();
        if (expectedAuthor != null && !containsIgnoreCase(event.getAuthor(), expectedAuthor)) {
            return false;
        }

        String expectedGenre = subscription.getGenre();
        return expectedGenre == null || equalsIgnoreCase(expectedGenre, event.getGenre());
    }

    private String buildMessage(Book book) {
        return "Wishlist match found: '" + book.getTitle() + "' by " + book.getAuthor() + " is now available for exchange.";
    }

    private boolean containsIgnoreCase(String source, String value) {
        if (source == null || value == null) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.equalsIgnoreCase(right);
    }
}
