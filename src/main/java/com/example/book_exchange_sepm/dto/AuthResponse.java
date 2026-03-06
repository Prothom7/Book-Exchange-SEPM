package com.example.book_exchange_sepm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private Long id;
    private String username;
    private String email;
    private Set<String> roles;
    private String message;
    private String token;
    private Boolean emailVerified;

    public AuthResponse(String message) {
        this.message = message;
    }
    
    public AuthResponse(Long id, String username, String email, Set<String> roles, String message) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.message = message;
    }
}
