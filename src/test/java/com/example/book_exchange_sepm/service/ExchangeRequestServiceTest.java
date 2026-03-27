package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.ExchangeRequestRequest;
import com.example.book_exchange_sepm.dto.ExchangeRequestResponse;
import com.example.book_exchange_sepm.dto.ExchangeStatusUpdateRequest;
import com.example.book_exchange_sepm.entity.Book;
import com.example.book_exchange_sepm.entity.ExchangeRequest;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.exception.UnauthorizedActionException;
import com.example.book_exchange_sepm.repository.ExchangeRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRequestServiceTest {

    @Mock
    private ExchangeRequestRepository exchangeRequestRepository;

    @Mock
    private BookService bookService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ExchangeRequestService exchangeRequestService;

    @Test
    void createExchangeRequest_ShouldCreatePendingRequest_WhenValid() {
        User requester = user(1L, "requester");
        User owner = user(2L, "owner");

        Book requested = book(10L, owner, true);
        Book offered = book(20L, requester, true);

        ExchangeRequestRequest request = new ExchangeRequestRequest();
        request.setBookId(10L);
        request.setOfferedBookId(20L);
        request.setMessage("Can we exchange?");

        when(userService.getCurrentUserEntity()).thenReturn(requester);
        when(bookService.findBookById(10L)).thenReturn(requested);
        when(bookService.findBookById(20L)).thenReturn(offered);
        when(exchangeRequestRepository.save(any(ExchangeRequest.class))).thenAnswer(invocation -> {
            ExchangeRequest saved = invocation.getArgument(0);
            saved.setId(500L);
            return saved;
        });

        ExchangeRequestResponse response = exchangeRequestService.createExchangeRequest(request);

        assertEquals("PENDING", response.getStatus());
        assertEquals(10L, response.getBookId());
        assertEquals(20L, response.getOfferedBookId());
    }

    @Test
    void createExchangeRequest_ShouldThrow_WhenUserRequestsOwnBook() {
        User requester = user(1L, "requester");

        Book requested = book(10L, requester, true);
        Book offered = book(20L, requester, true);

        ExchangeRequestRequest request = new ExchangeRequestRequest();
        request.setBookId(10L);
        request.setOfferedBookId(20L);

        when(userService.getCurrentUserEntity()).thenReturn(requester);
        when(bookService.findBookById(10L)).thenReturn(requested);
        when(bookService.findBookById(20L)).thenReturn(offered);

        assertThrows(UnauthorizedActionException.class, () -> exchangeRequestService.createExchangeRequest(request));
    }

    @Test
    void createExchangeRequest_ShouldThrow_WhenOfferedBookNotOwnedByRequester() {
        User requester = user(1L, "requester");
        User owner = user(2L, "owner");

        Book requested = book(10L, owner, true);
        Book offered = book(20L, owner, true);

        ExchangeRequestRequest request = new ExchangeRequestRequest();
        request.setBookId(10L);
        request.setOfferedBookId(20L);

        when(userService.getCurrentUserEntity()).thenReturn(requester);
        when(bookService.findBookById(10L)).thenReturn(requested);
        when(bookService.findBookById(20L)).thenReturn(offered);

        assertThrows(UnauthorizedActionException.class, () -> exchangeRequestService.createExchangeRequest(request));
    }

    @Test
    void approveExchangeRequest_ShouldApproveAndMarkBooksUnavailable() {
        User requester = user(1L, "requester");
        User owner = user(2L, "owner");
        User moderator = user(3L, "mod");

        ExchangeRequest exchange = exchange(101L, requester, owner, moderator, ExchangeRequest.Status.PENDING);

        when(exchangeRequestRepository.findById(101L)).thenReturn(Optional.of(exchange));
        when(userService.isModerator()).thenReturn(true);
        when(userService.getCurrentUserEntity()).thenReturn(moderator);
        when(exchangeRequestRepository.save(exchange)).thenReturn(exchange);

        ExchangeRequestResponse response = exchangeRequestService.approveExchangeRequest(101L);

        assertEquals("APPROVED", response.getStatus());
        assertTrue(Boolean.FALSE.equals(exchange.getBook().getAvailable()));
        assertTrue(Boolean.FALSE.equals(exchange.getOfferedBook().getAvailable()));
    }

    @Test
    void rejectExchangeRequest_ShouldThrow_WhenNotPending() {
        User requester = user(1L, "requester");
        User owner = user(2L, "owner");
        User moderator = user(3L, "mod");

        ExchangeRequest exchange = exchange(102L, requester, owner, moderator, ExchangeRequest.Status.APPROVED);

        when(exchangeRequestRepository.findById(102L)).thenReturn(Optional.of(exchange));
        when(userService.isModerator()).thenReturn(true);

        assertThrows(UnauthorizedActionException.class, () -> exchangeRequestService.rejectExchangeRequest(102L));
    }

    @Test
    void cancelExchangeRequest_ShouldThrow_WhenRequesterDoesNotOwnRequest() {
        User requester = user(1L, "requester");
        User owner = user(2L, "owner");

        ExchangeRequest exchange = exchange(103L, requester, owner, null, ExchangeRequest.Status.PENDING);

        when(exchangeRequestRepository.findById(103L)).thenReturn(Optional.of(exchange));
        when(userService.getCurrentUserId()).thenReturn(999L);

        assertThrows(UnauthorizedActionException.class, () -> exchangeRequestService.cancelExchangeRequest(103L));
    }

    @Test
    void updateRequestStatus_ShouldThrow_WhenInvalidStatusValue() {
        User requester = user(1L, "requester");
        User owner = user(2L, "owner");

        ExchangeRequest exchange = exchange(104L, requester, owner, null, ExchangeRequest.Status.PENDING);

        ExchangeStatusUpdateRequest request = new ExchangeStatusUpdateRequest();
        request.setStatus("DONE");

        when(exchangeRequestRepository.findById(104L)).thenReturn(Optional.of(exchange));
        when(userService.isModerator()).thenReturn(true);

        assertThrows(UnauthorizedActionException.class,
            () -> exchangeRequestService.updateRequestStatus(104L, request));
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        return user;
    }

    private Book book(Long id, User owner, boolean available) {
        Book book = new Book();
        book.setId(id);
        book.setTitle("Book " + id);
        book.setAuthor("Author " + id);
        book.setGenre("Genre");
        book.setLanguage("English");
        book.setIsbn("9780000000" + id);
        book.setBookCondition("Good");
        book.setDescription("Description");
        book.setOwner(owner);
        book.setAvailable(available);
        return book;
    }

    private ExchangeRequest exchange(Long id,
                                     User requester,
                                     User owner,
                                     User reviewer,
                                     ExchangeRequest.Status status) {
        ExchangeRequest request = new ExchangeRequest();
        request.setId(id);
        request.setRequester(requester);
        request.setBook(book(id + 10, owner, true));
        request.setOfferedBook(book(id + 20, requester, true));
        request.setStatus(status);
        request.setMessage("msg");
        request.setReviewedBy(reviewer);
        return request;
    }
}
