package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.dto.DeliveryResponse;
import com.example.book_exchange_sepm.dto.DeliveryStatusUpdateRequest;
import com.example.book_exchange_sepm.entity.Delivery;
import com.example.book_exchange_sepm.exception.UnauthorizedActionException;
import com.example.book_exchange_sepm.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery")
@PreAuthorize("hasRole('DELIVERY_MAN')")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/my-assignments")
    public ResponseEntity<List<DeliveryResponse>> getMyAssignments() {
        return new ResponseEntity<>(deliveryService.getAssignedDeliveriesForCurrentDeliveryMan(), HttpStatus.OK);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DeliveryResponse> updateStatus(@PathVariable Long id,
                                                         @Valid @RequestBody DeliveryStatusUpdateRequest request) {
        Delivery.Status status;
        try {
            status = Delivery.Status.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedActionException("Invalid delivery status");
        }
        return new ResponseEntity<>(deliveryService.updateDeliveryStatus(id, status), HttpStatus.OK);
    }
}
