package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.controller.form.BookSearchForm;
import com.example.book_exchange_sepm.dto.BookRequest;
import com.example.book_exchange_sepm.dto.BookResponse;
import com.example.book_exchange_sepm.entity.Book;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.exception.ResourceNotFoundException;
import com.example.book_exchange_sepm.exception.UnauthorizedActionException;
import com.example.book_exchange_sepm.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final UserService userService;

    public BookService(BookRepository bookRepository, UserService userService) {
        this.bookRepository = bookRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public List<String> getGenres() {
        return bookRepository.findDistinctGenres();
    }

    @Transactional(readOnly = true)
    public List<String> getLanguages() {
        return bookRepository.findDistinctLanguages();
    }

    @Transactional
    public BookResponse createBook(BookRequest request) {
        User currentUser = userService.getCurrentUserEntity();

        Book book = new Book();
        applyBookRequest(book, request);
        book.setOwner(currentUser);
        book.setAvailable(true);

        Book savedBook = bookRepository.save(book);
        return convertToResponse(savedBook);
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAllByOrderByTitleAsc().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookResponse getBookById(Long bookId) {
        return convertToResponse(findBookById(bookId));
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getAvailableBooks() {
        return bookRepository.findByAvailableTrue().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getUserBooks(Long userId) {
        User owner = userService.findById(userId);
        return bookRepository.findByOwner(owner).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getMyBooks() {
        User currentUser = userService.getCurrentUserEntity();
        return bookRepository.findByOwner(currentUser).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public BookResponse updateBook(Long bookId, BookRequest request) {
        Book book = findBookById(bookId);
        userService.validateOwnershipOrAdmin(book.getOwner().getId());
        applyBookRequest(book, request);
        return convertToResponse(bookRepository.save(book));
    }

    @Transactional
    public void deleteBook(Long bookId) {
        Book book = findBookById(bookId);
        userService.validateOwnershipOrAdmin(book.getOwner().getId());
        bookRepository.delete(book);
    }

    @Transactional
    public void deleteBookAsModerator(Long bookId) {
        if (!userService.isModerator()) {
            throw new UnauthorizedActionException("Only moderators can perform this action");
        }

        bookRepository.delete(findBookById(bookId));
    }

    @Transactional
    public BookResponse markBookAvailability(Long bookId, Boolean available) {
        Book book = findBookById(bookId);
        userService.validateOwnershipOrAdmin(book.getOwner().getId());
        book.setAvailable(available);
        return convertToResponse(bookRepository.save(book));
    }

    @Transactional(readOnly = true)
    protected Book findBookById(Long bookId) {
        return bookRepository.findById(bookId)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
    }

    private void applyBookRequest(Book book, BookRequest request) {
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setDescription(request.getDescription());
        book.setIsbn(request.getIsbn());
        book.setBookCondition(request.getCondition());

        if (book.getGenre() == null) {
            book.setGenre("General");
        }
        if (book.getLanguage() == null) {
            book.setLanguage("English");
        }
        if (book.getPublicationYear() == null) {
            book.setPublicationYear(0);
        }
    }

    private BookResponse convertToResponse(Book book) {
        return new BookResponse(
            book.getId(),
            book.getTitle(),
            book.getAuthor(),
            book.getDescription(),
            book.getIsbn(),
            book.getOwner() != null ? book.getOwner().getId() : null,
            book.getOwner() != null ? book.getOwner().getUsername() : null,
            book.getBookCondition(),
            book.getAvailable(),
            book.getCreatedAt(),
            book.getUpdatedAt()
        );
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
