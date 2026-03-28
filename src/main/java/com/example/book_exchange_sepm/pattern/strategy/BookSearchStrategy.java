package com.example.book_exchange_sepm.pattern.strategy;

import com.example.book_exchange_sepm.entity.Book;

public interface BookSearchStrategy {

    boolean matches(Book book, String query);
}
