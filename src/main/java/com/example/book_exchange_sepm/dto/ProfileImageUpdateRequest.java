package com.example.book_exchange_sepm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileImageUpdateRequest {

    @NotBlank(message = "Image data is required")
    private String imageDataUrl;
}
