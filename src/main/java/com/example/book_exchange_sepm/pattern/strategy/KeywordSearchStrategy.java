package com.example.book_exchange_sepm.pattern.strategy;

import com.example.book_exchange_sepm.entity.Book;
import org.springframework.stereotype.Component;

@Component
public class KeywordSearchStrategy implements BookSearchStrategy {

    @Override
    public boolean matches(Book book, String query) {
        return containsIgnoreCase(book.getTitle(), query)
            || containsIgnoreCase(book.getAuthor(), query)
            || containsIgnoreCase(book.getGenre(), query)
            || containsIgnoreCase(book.getDescription(), query)
            || containsIgnoreCase(book.getIsbn(), query);
    }

    private boolean containsIgnoreCase(String source, String query) {
        return source != null && query != null && source.toLowerCase().contains(query.toLowerCase());
    }
}
