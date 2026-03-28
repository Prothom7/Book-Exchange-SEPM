package com.example.book_exchange_sepm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status;

    private String moderatorComment;
}
