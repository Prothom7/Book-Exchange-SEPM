package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.BookRequest;
import com.example.book_exchange_sepm.dto.BookResponse;
import com.example.book_exchange_sepm.entity.Book;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.exception.UnauthorizedActionException;
import com.example.book_exchange_sepm.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private BookService bookService;

    @Test
    void createBook_ShouldAssignCurrentUserAsOwnerAndAvailableTrue() {
        User currentUser = new User();
        currentUser.setId(7L);
        currentUser.setUsername("owner-user");

        BookRequest request = new BookRequest(
            "Clean Code",
            "Robert C. Martin",
            "A software craftsmanship classic",
            "9780132350884",
            "Good"
        );

        when(userService.getCurrentUserEntity()).thenReturn(currentUser);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        BookResponse response = bookService.createBook(request);

        assertEquals(101L, response.getId());
        assertEquals(7L, response.getOwnerId());
        assertEquals("owner-user", response.getOwnerUsername());
        assertTrue(response.getAvailable());
    }

    @Test
    void markBookAvailability_ShouldUpdateAvailability_WhenOwnerIsAuthorized() {
        User owner = new User();
        owner.setId(7L);
        owner.setUsername("owner-user");

        Book book = new Book();
        book.setId(55L);
        book.setOwner(owner);
        book.setTitle("Domain-Driven Design");
        book.setAuthor("Eric Evans");
        book.setIsbn("9780321125217");
        book.setCondition("Excellent");
        book.setAvailable(true);

        when(bookRepository.findById(55L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse response = bookService.markBookAvailability(55L, false);

        verify(userService).validateOwnershipOrAdmin(7L);
        assertFalse(response.getAvailable());
    }

    @Test
    void updateBook_ShouldFail_WhenOwnershipValidationFails() {
        User owner = new User();
        owner.setId(15L);
        owner.setUsername("another-owner");

        Book book = new Book();
        book.setId(70L);
        book.setOwner(owner);
        book.setTitle("Old title");
        book.setAuthor("Old author");
        book.setIsbn("1111111111");
        book.setCondition("Fair");

        BookRequest request = new BookRequest("New title", "New author", "desc", "2222222222", "Good");

        when(bookRepository.findById(70L)).thenReturn(Optional.of(book));
        doThrow(new UnauthorizedActionException("You do not have permission to modify this resource"))
            .when(userService).validateOwnershipOrAdmin(15L);

        assertThrows(UnauthorizedActionException.class, () -> bookService.updateBook(70L, request));
    }

    @Test
    void deleteBookAsModerator_ShouldThrow_WhenCurrentUserIsNotModerator() {
        when(userService.isModerator()).thenReturn(false);

        assertThrows(UnauthorizedActionException.class, () -> bookService.deleteBookAsModerator(123L));
    }
}
