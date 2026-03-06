package com.example.book_exchange_sepm.service.impl;

import com.example.book_exchange_sepm.controller.form.BookSearchForm;
import com.example.book_exchange_sepm.model.Book;
import com.example.book_exchange_sepm.repository.BookRepository;
import com.example.book_exchange_sepm.service.BookService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    public BookServiceImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public List<Book> searchBooks(BookSearchForm searchForm) {
        String keyword = normalize(searchForm.getKeyword());
        String title = normalize(searchForm.getTitle());
        String author = normalize(searchForm.getAuthor());
        String genre = normalize(searchForm.getGenre());
        String language = normalize(searchForm.getLanguage());
        String isbn = normalize(searchForm.getIsbn());
        String bookCondition = normalize(searchForm.getBookCondition());
        Integer minYear = searchForm.getMinYear();
        Integer maxYear = searchForm.getMaxYear();
        boolean availableOnly = searchForm.isAvailableOnly();

        Stream<Book> stream = bookRepository.findAllByOrderByTitleAsc().stream();

        if (keyword != null) {
            stream = stream.filter(book -> containsIgnoreCase(book.getTitle(), keyword)
                    || containsIgnoreCase(book.getAuthor(), keyword)
                    || containsIgnoreCase(book.getGenre(), keyword)
                    || containsIgnoreCase(book.getLanguage(), keyword)
                    || containsIgnoreCase(book.getIsbn(), keyword));
        }
        if (title != null) {
            stream = stream.filter(book -> containsIgnoreCase(book.getTitle(), title));
        }
        if (author != null) {
            stream = stream.filter(book -> containsIgnoreCase(book.getAuthor(), author));
        }
        if (genre != null) {
            stream = stream.filter(book -> equalsIgnoreCase(book.getGenre(), genre));
        }
        if (language != null) {
            stream = stream.filter(book -> equalsIgnoreCase(book.getLanguage(), language));
        }
        if (isbn != null) {
            stream = stream.filter(book -> containsIgnoreCase(book.getIsbn(), isbn));
        }
        if (bookCondition != null) {
            stream = stream.filter(book -> equalsIgnoreCase(book.getBookCondition(), bookCondition));
        }
        if (minYear != null) {
            stream = stream.filter(book -> book.getPublicationYear() != null && book.getPublicationYear() >= minYear);
        }
        if (maxYear != null) {
            stream = stream.filter(book -> book.getPublicationYear() != null && book.getPublicationYear() <= maxYear);
        }
        if (availableOnly) {
            stream = stream.filter(book -> Boolean.TRUE.equals(book.getAvailable()));
        }

        return stream.toList();
    }

    @Override
    public List<String> getGenres() {
        return bookRepository.findDistinctGenres();
    }

    @Override
    public List<String> getLanguages() {
        return bookRepository.findDistinctLanguages();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean containsIgnoreCase(String source, String query) {
        if (source == null || query == null) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.equalsIgnoreCase(right);
    }
}
