package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.dto.WishlistSubscriptionRequest;
import com.example.book_exchange_sepm.dto.WishlistSubscriptionResponse;
import com.example.book_exchange_sepm.service.WishlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist-rest")
public class WishlistRestController {

    private final WishlistService wishlistService;

    public WishlistRestController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<WishlistSubscriptionResponse> addWishlist(@Valid @RequestBody WishlistSubscriptionRequest request) {
        return new ResponseEntity<>(wishlistService.subscribe(request), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<List<WishlistSubscriptionResponse>> getWishlist() {
        return new ResponseEntity<>(wishlistService.getMySubscriptions(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Void> removeWishlist(@PathVariable Long id) {
        wishlistService.removeSubscription(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
