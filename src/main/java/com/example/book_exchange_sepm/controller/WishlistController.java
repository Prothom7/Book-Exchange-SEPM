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
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @PostMapping("/subscribe")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<WishlistSubscriptionResponse> subscribe(
        @Valid @RequestBody WishlistSubscriptionRequest request
    ) {
        WishlistSubscriptionResponse response = wishlistService.subscribe(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<List<WishlistSubscriptionResponse>> getMySubscriptions() {
        return new ResponseEntity<>(wishlistService.getMySubscriptions(), HttpStatus.OK);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<WishlistSubscriptionResponse> deactivate(@PathVariable Long id) {
        return new ResponseEntity<>(wishlistService.deactivateSubscription(id), HttpStatus.OK);
    }
}
