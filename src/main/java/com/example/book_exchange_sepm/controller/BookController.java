package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.dto.BookRequest;
import com.example.book_exchange_sepm.dto.BookResponse;
import com.example.book_exchange_sepm.service.BookService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;

    /**
     * Create new book (USER+ roles)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookRequest request) {
        BookResponse response = bookService.createBook(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all books (authenticated users)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<List<BookResponse>> getAllBooks() {
        List<BookResponse> books = bookService.getAllBooks();
        return new ResponseEntity<>(books, HttpStatus.OK);
    }

    /**
     * Get book by ID (authenticated users)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<BookResponse> getBookById(@PathVariable Long id) {
        BookResponse book = bookService.getBookById(id);
        return new ResponseEntity<>(book, HttpStatus.OK);
    }

    /**
     * Get available books
     */
    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<List<BookResponse>> getAvailableBooks() {
        List<BookResponse> books = bookService.getAvailableBooks();
        return new ResponseEntity<>(books, HttpStatus.OK);
    }

    /**
     * Get current user's books
     */
    @GetMapping("/my-books")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<List<BookResponse>> getMyBooks() {
        List<BookResponse> books = bookService.getMyBooks();
        return new ResponseEntity<>(books, HttpStatus.OK);
    }

    /**
     * Update book (ownership enforced)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<BookResponse> updateBook(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        BookResponse response = bookService.updateBook(id, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Delete book (ownership enforced - owner or admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Mark book availability (ownership enforced)
     */
    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<BookResponse> markAvailability(@PathVariable Long id, @RequestParam Boolean available) {
        BookResponse response = bookService.markBookAvailability(id, available);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
