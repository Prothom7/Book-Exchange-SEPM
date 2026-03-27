package com.example.book_exchange_sepm.pattern.factory;

import com.example.book_exchange_sepm.entity.Book;
import com.example.book_exchange_sepm.entity.ExchangeRequest;
import com.example.book_exchange_sepm.entity.User;

import java.time.LocalDateTime;

public class ExchangeRequestFactory {

    public ExchangeRequest createPending(User requester, Book requestedBook, Book offeredBook, String message) {
        return create(requester, requestedBook, offeredBook, message, ExchangeRequest.Status.PENDING, null, null);
    }

    public ExchangeRequest createReviewed(User requester,
                                          Book requestedBook,
                                          Book offeredBook,
                                          String message,
                                          ExchangeRequest.Status status,
                                          User reviewer,
                                          String moderatorComment) {
        return create(requester, requestedBook, offeredBook, message, status, reviewer, moderatorComment);
    }

    private ExchangeRequest create(User requester,
                                   Book requestedBook,
                                   Book offeredBook,
                                   String message,
                                   ExchangeRequest.Status status,
                                   User reviewer,
                                   String moderatorComment) {
        ExchangeRequest exchangeRequest = new ExchangeRequest();
        exchangeRequest.setRequester(requester);
        exchangeRequest.setBook(requestedBook);
        exchangeRequest.setOfferedBook(offeredBook);
        exchangeRequest.setMessage(message);
        exchangeRequest.setStatus(status);
        exchangeRequest.setReviewedBy(reviewer);
        exchangeRequest.setModeratorComment(moderatorComment);
        exchangeRequest.setReviewedAt(reviewer != null ? LocalDateTime.now() : null);
        return exchangeRequest;
    }
}
