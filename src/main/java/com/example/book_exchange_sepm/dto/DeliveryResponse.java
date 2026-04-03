package com.example.book_exchange_sepm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResponse {

    private Long id;
    private Long exchangeRequestId;
    private String requestedBookTitle;
    private String offeredBookTitle;
    private String requesterUsername;
    private String ownerUsername;
    private String deliveryManUsername;
    private String status;
    private String notes;
    private LocalDateTime assignedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime updatedAt;
}
