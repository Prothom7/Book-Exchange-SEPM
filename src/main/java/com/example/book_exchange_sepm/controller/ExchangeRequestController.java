package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.dto.ExchangeRequestRequest;
import com.example.book_exchange_sepm.dto.ExchangeRequestResponse;
import com.example.book_exchange_sepm.service.ExchangeRequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exchange-requests")
public class ExchangeRequestController {

    @Autowired
    private ExchangeRequestService exchangeRequestService;

    /**
     * Create exchange request (USER+ roles)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<ExchangeRequestResponse> createExchangeRequest(
            @Valid @RequestBody ExchangeRequestRequest request) {
        ExchangeRequestResponse response = exchangeRequestService.createExchangeRequest(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get my books' exchange requests (list of requests for books I own)
     */
    @GetMapping("/my-book-requests")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<List<ExchangeRequestResponse>> getMyBookExchangeRequests() {
        List<ExchangeRequestResponse> requests = exchangeRequestService.getMyBookExchangeRequests();
        return new ResponseEntity<>(requests, HttpStatus.OK);
    }

    /**
     * Get my exchange requests (requests I made)
     */
    @GetMapping("/my-requests")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<List<ExchangeRequestResponse>> getMyExchangeRequests() {
        List<ExchangeRequestResponse> requests = exchangeRequestService.getMyExchangeRequests();
        return new ResponseEntity<>(requests, HttpStatus.OK);
    }

    /**
     * Get exchange request by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<ExchangeRequestResponse> getExchangeRequestById(@PathVariable Long id) {
        ExchangeRequestResponse response = exchangeRequestService.getExchangeRequestById(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Approve exchange request (ownership enforced)
     * Only book owner can approve
     */
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<ExchangeRequestResponse> approveExchangeRequest(@PathVariable Long id) {
        ExchangeRequestResponse response = exchangeRequestService.approveExchangeRequest(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Reject exchange request (ownership enforced)
     * Only book owner can reject
     */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<ExchangeRequestResponse> rejectExchangeRequest(@PathVariable Long id) {
        ExchangeRequestResponse response = exchangeRequestService.rejectExchangeRequest(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Cancel exchange request
     * Only requester can cancel their own request
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<ExchangeRequestResponse> cancelExchangeRequest(@PathVariable Long id) {
        ExchangeRequestResponse response = exchangeRequestService.cancelExchangeRequest(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
