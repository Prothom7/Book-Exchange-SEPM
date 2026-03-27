package com.example.book_exchange_sepm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistSubscriptionResponse {

    private Long id;
    private String bookTitle;
    private String author;
    private String genre;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
