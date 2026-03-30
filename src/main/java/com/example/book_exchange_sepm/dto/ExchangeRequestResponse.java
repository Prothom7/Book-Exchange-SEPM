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
    private String bookImageUrl;
    private Long bookOwnerId;
    private String bookOwnerUsername;
    private Long offeredBookId;
    private String offeredBookTitle;
    private String offeredBookImageUrl;
    private String status;
    private String message;
    private String moderatorComment;
    private Long reviewedById;
    private String reviewedByUsername;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
