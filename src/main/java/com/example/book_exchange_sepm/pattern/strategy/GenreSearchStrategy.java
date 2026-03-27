package com.example.book_exchange_sepm.pattern.strategy;

import com.example.book_exchange_sepm.entity.Book;
import org.springframework.stereotype.Component;

@Component
public class GenreSearchStrategy implements BookSearchStrategy {

    @Override
    public boolean matches(Book book, String query) {
        return equalsIgnoreCase(book.getGenre(), query);
    }

    private boolean equalsIgnoreCase(String source, String query) {
        return source != null && query != null && source.equalsIgnoreCase(query);
    }
}
