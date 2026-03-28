package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.dto.ExchangeRequestRequest;
import com.example.book_exchange_sepm.dto.ExchangeRequestResponse;
import com.example.book_exchange_sepm.dto.ExchangeStatusUpdateRequest;
import com.example.book_exchange_sepm.service.ExchangeRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exchange-rest")
public class ExchangeRestController {

    private final ExchangeRequestService exchangeRequestService;

    public ExchangeRestController(ExchangeRequestService exchangeRequestService) {
        this.exchangeRequestService = exchangeRequestService;
    }

    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<ExchangeRequestResponse> requestExchange(@Valid @RequestBody ExchangeRequestRequest request) {
        return new ResponseEntity<>(exchangeRequestService.createExchangeRequest(request), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<List<ExchangeRequestResponse>> getMyRequests() {
        return new ResponseEntity<>(exchangeRequestService.getMyExchangeRequests(), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ExchangeRequestResponse> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody ExchangeStatusUpdateRequest request
    ) {
        return new ResponseEntity<>(exchangeRequestService.updateRequestStatus(id, request), HttpStatus.OK);
    }
}
