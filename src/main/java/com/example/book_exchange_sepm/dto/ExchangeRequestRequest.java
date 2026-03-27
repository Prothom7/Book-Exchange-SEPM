package com.example.book_exchange_sepm.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRequestRequest {

    @NotNull(message = "Book ID is required")
    private Long bookId;

    @NotNull(message = "Offered book ID is required")
    private Long offeredBookId;

    private String message;
}
