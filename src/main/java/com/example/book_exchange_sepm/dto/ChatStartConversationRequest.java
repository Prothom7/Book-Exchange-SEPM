package com.example.book_exchange_sepm.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatStartConversationRequest {

    @NotBlank
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
