package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.dto.BookRequest;
import com.example.book_exchange_sepm.dto.BookResponse;
import com.example.book_exchange_sepm.pattern.strategy.SearchMode;
import com.example.book_exchange_sepm.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books-rest")
public class BooksRestController {

    private final BookService bookService;

    public BooksRestController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<BookResponse> addBook(@Valid @RequestBody BookRequest request) {
        return new ResponseEntity<>(bookService.createBook(request), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<List<BookResponse>> browseAvailableBooks() {
        return new ResponseEntity<>(bookService.getAvailableBooks(), HttpStatus.OK);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<List<BookResponse>> searchBooks(
        @RequestParam("q") String query,
        @RequestParam(value = "mode", required = false, defaultValue = "KEYWORD") SearchMode mode
    ) {
        return new ResponseEntity<>(bookService.searchAvailableBooks(query, mode), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<BookResponse> getBookDetails(@PathVariable Long id) {
        return new ResponseEntity<>(bookService.getBookById(id), HttpStatus.OK);
    }
}
