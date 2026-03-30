package com.example.book_exchange_sepm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {

    @NotBlank(message = "Message content is required")
    @Size(min = 1, max = 1200, message = "Message must be between 1 and 1200 characters")
    private String content;
}
