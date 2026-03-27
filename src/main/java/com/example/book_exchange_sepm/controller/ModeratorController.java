package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/moderator")
public class ModeratorController {

    @Autowired
    private BookService bookService;

    /**
     * Delete inappropriate book (moderator only)
     * Moderators can delete any book for platform safety
     */
    @DeleteMapping("/books/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> deleteInappropriateBook(@PathVariable Long id) {
        bookService.deleteBookAsModerator(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Moderator dashboard status
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<String> moderatorDashboard() {
        return new ResponseEntity<>("Welcome to Moderator Dashboard", HttpStatus.OK);
    }
}
