package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.BookRequest;
import com.example.book_exchange_sepm.dto.BookResponse;
import com.example.book_exchange_sepm.controller.form.BookSearchForm;
import com.example.book_exchange_sepm.entity.Book;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.exception.UnauthorizedActionException;
import com.example.book_exchange_sepm.pattern.strategy.BookSearchStrategyResolver;
import com.example.book_exchange_sepm.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Mock
    private BookSearchStrategyResolver strategyResolver;

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
        assertNotNull(response.getImageUrl());
    }

    @Test
    void getAvailableBooks_ShouldReturnOnlyAvailableBooks() {
        Book available = new Book();
        available.setId(1L);
        available.setTitle("Available Book");
        available.setAuthor("Author A");
        available.setGenre("Fiction");
        available.setLanguage("English");
        available.setIsbn("9780000000001");
        available.setBookCondition("Good");
        available.setDescription("Desc");
        available.setAvailable(true);

        User owner = new User();
        owner.setId(10L);
        owner.setUsername("owner");
        available.setOwner(owner);

        when(bookRepository.findByAvailableTrue()).thenReturn(List.of(available));

        List<BookResponse> result = bookService.getAvailableBooks();
        assertEquals(1, result.size());
        assertEquals("Available Book", result.get(0).getTitle());
    }

    @Test
    void searchBooks_ShouldIncludeOnlyAvailableEvenWhenAvailableOnlyIsFalse() {
        Book available = new Book();
        available.setId(1L);
        available.setTitle("Clean Architecture");
        available.setAuthor("Robert Martin");
        available.setGenre("Software");
        available.setLanguage("English");
        available.setIsbn("9780134494166");
        available.setBookCondition("Good");
        available.setDescription("desc");
        available.setAvailable(true);

        Book unavailable = new Book();
        unavailable.setId(2L);
        unavailable.setTitle("Unavailable Book");
        unavailable.setAuthor("Some Author");
        unavailable.setGenre("Fiction");
        unavailable.setLanguage("English");
        unavailable.setIsbn("9780000000002");
        unavailable.setBookCondition("Fair");
        unavailable.setDescription("desc");
        unavailable.setAvailable(false);

        when(bookRepository.findAllByOrderByTitleAsc()).thenReturn(List.of(available, unavailable));

        BookSearchForm form = new BookSearchForm();
        form.setAvailableOnly(false);

        List<Book> result = bookService.searchBooks(form);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void searchBooks_ShouldFilterByKeyword() {
        Book dune = new Book();
        dune.setId(1L);
        dune.setTitle("Dune");
        dune.setAuthor("Frank Herbert");
        dune.setGenre("Science Fiction");
        dune.setLanguage("English");
        dune.setIsbn("9780441172719");
        dune.setBookCondition("Good");
        dune.setDescription("Desert planet");
        dune.setAvailable(true);

        Book random = new Book();
        random.setId(2L);
        random.setTitle("Cooking 101");
        random.setAuthor("Chef");
        random.setGenre("Cooking");
        random.setLanguage("English");
        random.setIsbn("9780000000003");
        random.setBookCondition("Good");
        random.setDescription("Kitchen basics");
        random.setAvailable(true);

        when(bookRepository.findAllByOrderByTitleAsc()).thenReturn(List.of(dune, random));

        BookSearchForm form = new BookSearchForm();
        form.setKeyword("dune");

        List<Book> result = bookService.searchBooks(form);
        assertEquals(1, result.size());
        assertEquals("Dune", result.get(0).getTitle());
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
        book.setBookCondition("Excellent");
        book.setGenre("Software Engineering");
        book.setLanguage("English");
        book.setPublicationYear(2003);
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
        book.setBookCondition("Fair");
        book.setGenre("General");
        book.setLanguage("English");
        book.setPublicationYear(2000);

        BookRequest request = new BookRequest("New title", "New author", "desc", "2222222222", "Good");

        when(bookRepository.findById(70L)).thenReturn(Optional.of(book));
        doThrow(new UnauthorizedActionException("You do not have permission to modify this resource"))
            .when(userService).validateOwnershipOrAdmin(15L);

        assertThrows(UnauthorizedActionException.class, () -> bookService.updateBook(70L, request));
    }

    @Test
    void deleteBook_ShouldValidateOwnershipAndDelete() {
        User owner = new User();
        owner.setId(17L);

        Book book = new Book();
        book.setId(88L);
        book.setOwner(owner);

        when(bookRepository.findById(88L)).thenReturn(Optional.of(book));

        bookService.deleteBook(88L);

        verify(userService).validateOwnershipOrAdmin(17L);
        verify(bookRepository).delete(book);
    }

    @Test
    void deleteBookAsModerator_ShouldThrow_WhenCurrentUserIsNotModerator() {
        when(userService.isModerator()).thenReturn(false);

        assertThrows(UnauthorizedActionException.class, () -> bookService.deleteBookAsModerator(123L));
    }

    @Test
    void deleteBookAsModerator_ShouldDelete_WhenCurrentUserIsModerator() {
        when(userService.isModerator()).thenReturn(true);

        User owner = new User();
        owner.setId(22L);

        Book book = new Book();
        book.setId(123L);
        book.setOwner(owner);

        when(bookRepository.findById(123L)).thenReturn(Optional.of(book));

        bookService.deleteBookAsModerator(123L);

        verify(bookRepository).delete(book);
    }
}
