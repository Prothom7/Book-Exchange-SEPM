package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.controller.form.BookSearchForm;
import com.example.book_exchange_sepm.model.Book;

import java.util.List;

public interface BookService {

    List<Book> searchBooks(BookSearchForm searchForm);

    List<String> getGenres();

    List<String> getLanguages();
}
