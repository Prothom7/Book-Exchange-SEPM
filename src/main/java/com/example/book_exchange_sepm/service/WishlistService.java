package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.WishlistSubscriptionRequest;
import com.example.book_exchange_sepm.dto.WishlistSubscriptionResponse;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.entity.WishlistSubscription;
import com.example.book_exchange_sepm.exception.ResourceNotFoundException;
import com.example.book_exchange_sepm.exception.UnauthorizedActionException;
import com.example.book_exchange_sepm.repository.WishlistSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class WishlistService {

    private final WishlistSubscriptionRepository wishlistSubscriptionRepository;
    private final UserService userService;

    public WishlistService(WishlistSubscriptionRepository wishlistSubscriptionRepository,
                           UserService userService) {
        this.wishlistSubscriptionRepository = wishlistSubscriptionRepository;
        this.userService = userService;
    }

    @Transactional
    public WishlistSubscriptionResponse subscribe(WishlistSubscriptionRequest request) {
        User user = userService.getCurrentUserEntity();
        String normalizedTitle = normalizeRequired(request.getBookTitle());
        String normalizedAuthor = normalizeOptional(request.getAuthor());
        String normalizedGenre = normalizeOptional(request.getGenre());

        boolean duplicateExists = wishlistSubscriptionRepository.findByUserIdAndActiveTrue(user.getId()).stream()
            .anyMatch(item -> isSameSubscription(item, normalizedTitle, normalizedAuthor, normalizedGenre));

        if (duplicateExists) {
            throw new UnauthorizedActionException("You already have this active wishlist subscription");
        }

        WishlistSubscription subscription = new WishlistSubscription();
        subscription.setUser(user);
        subscription.setBookTitle(normalizedTitle);
        subscription.setAuthor(normalizedAuthor);
        subscription.setGenre(normalizedGenre);
        subscription.setActive(true);

        WishlistSubscription saved = wishlistSubscriptionRepository.save(subscription);
        return convertToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WishlistSubscriptionResponse> getMySubscriptions() {
        Long currentUserId = userService.getCurrentUserId();
        return wishlistSubscriptionRepository.findByUserIdOrderByCreatedAtDesc(currentUserId).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public WishlistSubscriptionResponse deactivateSubscription(Long subscriptionId) {
        Long currentUserId = userService.getCurrentUserId();
        WishlistSubscription subscription = wishlistSubscriptionRepository.findByIdAndUserId(subscriptionId, currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Wishlist subscription not found"));

        subscription.setActive(false);
        WishlistSubscription updated = wishlistSubscriptionRepository.save(subscription);
        return convertToResponse(updated);
    }

    /**
     * Remove (delete) a wishlist subscription
     */
    @Transactional
    public void removeSubscription(Long subscriptionId) {
        Long currentUserId = userService.getCurrentUserId();
        WishlistSubscription subscription = wishlistSubscriptionRepository.findByIdAndUserId(subscriptionId, currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Wishlist subscription not found"));

        wishlistSubscriptionRepository.delete(subscription);
    }

    private WishlistSubscriptionResponse convertToResponse(WishlistSubscription subscription) {
        return new WishlistSubscriptionResponse(
            subscription.getId(),
            subscription.getBookTitle(),
            subscription.getAuthor(),
            subscription.getGenre(),
            subscription.getActive(),
            subscription.getCreatedAt(),
            subscription.getUpdatedAt()
        );
    }

    private boolean isSameSubscription(WishlistSubscription item,
                                       String title,
                                       String author,
                                       String genre) {
        return equalsIgnoreCase(item.getBookTitle(), title)
            && equalsIgnoreCase(item.getAuthor(), author)
            && equalsIgnoreCase(item.getGenre(), genre);
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new UnauthorizedActionException("Book title is required");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.toLowerCase(Locale.ROOT).equals(right.toLowerCase(Locale.ROOT));
    }
}
