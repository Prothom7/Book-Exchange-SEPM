package com.example.book_exchange_sepm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {

    private Long id;
    private String title;
    private String author;
    private String description;
    private String isbn;
    private Long ownerId;
    private String ownerUsername;
    private String condition;
    private Boolean available;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
