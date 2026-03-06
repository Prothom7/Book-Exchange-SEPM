package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.BookRequest;
import com.example.book_exchange_sepm.dto.BookResponse;
import com.example.book_exchange_sepm.entity.Book;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.exception.ResourceNotFoundException;
import com.example.book_exchange_sepm.exception.UnauthorizedActionException;
import com.example.book_exchange_sepm.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserService userService;

    /**
     * Create a new book (accessible by USER and above)
     */
    @Transactional
    public BookResponse createBook(BookRequest request) {
        User currentUser = userService.getCurrentUserEntity();

        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setDescription(request.getDescription());
        book.setIsbn(request.getIsbn());
        book.setCondition(request.getCondition());
        book.setOwner(currentUser);
        book.setAvailable(true);

        Book savedBook = bookRepository.save(book);
        return convertToResponse(savedBook);
    }

    /**
     * Get all books (accessible by authenticated users)
     */
    @Transactional(readOnly = true)
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get book by ID (accessible by authenticated users)
     */
    @Transactional(readOnly = true)
    public BookResponse getBookById(Long bookId) {
        Book book = findBookById(bookId);
        return convertToResponse(book);
    }

    /**
     * Get currently available books
     */
    @Transactional(readOnly = true)
    public List<BookResponse> getAvailableBooks() {
        return bookRepository.findByAvailableTrue().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get books owned by specific user
     */
    @Transactional(readOnly = true)
    public List<BookResponse> getUserBooks(Long userId) {
        User owner = userService.findById(userId);
        return bookRepository.findByOwner(owner).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get current user's books
     */
    @Transactional(readOnly = true)
    public List<BookResponse> getMyBooks() {
        User currentUser = userService.getCurrentUserEntity();
        return bookRepository.findByOwner(currentUser).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Update book - OWNERSHIP ENFORCED
     * Only owner or ADMIN can update
     * Moderator cannot override ownership
     */
    @Transactional
    public BookResponse updateBook(Long bookId, BookRequest request) {
        Book book = findBookById(bookId);

        // Enforce ownership rule: only owner or admin can update
        userService.validateOwnershipOrAdmin(book.getOwner().getId());

        // Update fields
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setDescription(request.getDescription());
        book.setIsbn(request.getIsbn());
        book.setCondition(request.getCondition());

        Book updatedBook = bookRepository.save(book);
        return convertToResponse(updatedBook);
    }

    /**
     * Delete book - OWNERSHIP ENFORCED
     * Only owner or ADMIN can delete
     * Moderator cannot delete unless they are the owner
     */
    @Transactional
    public void deleteBook(Long bookId) {
        Book book = findBookById(bookId);

        // Enforce ownership rule: only owner or admin can delete
        userService.validateOwnershipOrAdmin(book.getOwner().getId());

        bookRepository.delete(book);
    }

    /**
     * Delete book by MODERATOR (as per role requirements)
     * Moderators can delete any book for platform safety
     * But they cannot manage other users' resources beyond deletion
     */
    @Transactional
    public void deleteBookAsModerator(Long bookId) {
        if (!userService.isModerator()) {
            throw new UnauthorizedActionException("Only moderators can perform this action");
        }

        Book book = findBookById(bookId);
        bookRepository.delete(book);
    }

    /**
     * Mark book as available/unavailable (owner only)
     */
    @Transactional
    public BookResponse markBookAvailability(Long bookId, Boolean available) {
        Book book = findBookById(bookId);

        // Only owner or admin can change availability
        userService.validateOwnershipOrAdmin(book.getOwner().getId());

        book.setAvailable(available);
        Book updatedBook = bookRepository.save(book);
        return convertToResponse(updatedBook);
    }

    /**
     * Find book by ID or throw exception
     */
    @Transactional(readOnly = true)
    protected Book findBookById(Long bookId) {
        return bookRepository.findById(bookId)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
    }

    /**
     * Convert Book entity to BookResponse DTO
     */
    private BookResponse convertToResponse(Book book) {
        return new BookResponse(
            book.getId(),
            book.getTitle(),
            book.getAuthor(),
            book.getDescription(),
            book.getIsbn(),
            book.getOwner().getId(),
            book.getOwner().getUsername(),
            book.getCondition(),
            book.getAvailable(),
            book.getCreatedAt(),
            book.getUpdatedAt()
        );
    }
}
