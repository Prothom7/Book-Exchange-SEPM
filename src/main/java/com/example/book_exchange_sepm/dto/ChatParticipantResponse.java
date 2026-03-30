package com.example.book_exchange_sepm.dto;

public class ChatParticipantResponse {

    private Long id;
    private String username;

    public ChatParticipantResponse() {
    }

    public ChatParticipantResponse(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
