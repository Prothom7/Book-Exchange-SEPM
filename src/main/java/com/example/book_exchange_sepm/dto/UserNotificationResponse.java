package com.example.book_exchange_sepm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationResponse {

    private Long id;
    private String message;
    private Long bookId;
    private String bookTitle;
    private Boolean read;
    private LocalDateTime createdAt;
}
