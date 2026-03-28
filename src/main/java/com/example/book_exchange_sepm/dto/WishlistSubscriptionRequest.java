package com.example.book_exchange_sepm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistSubscriptionRequest {

    @NotBlank(message = "Book title is required")
    private String bookTitle;

    private String author;

    private String genre;
}
