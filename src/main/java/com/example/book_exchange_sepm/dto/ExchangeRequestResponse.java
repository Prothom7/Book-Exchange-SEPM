package com.example.book_exchange_sepm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRequestResponse {

    private Long id;
    private Long requesterId;
    private String requesterUsername;
    private Long bookId;
    private String bookTitle;
    private Long bookOwnerId;
    private String bookOwnerUsername;
    private String status;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
