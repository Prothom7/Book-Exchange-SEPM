package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.BookRequest;
import com.example.book_exchange_sepm.dto.BookResponse;
import com.example.book_exchange_sepm.entity.Book;
import com.example.book_exchange_sepm.entity.Role;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.repository.BookRepository;
import com.example.book_exchange_sepm.repository.ExchangeRequestRepository;
import com.example.book_exchange_sepm.repository.RoleRepository;
import com.example.book_exchange_sepm.repository.UserNotificationRepository;
import com.example.book_exchange_sepm.repository.UserRepository;
import com.example.book_exchange_sepm.repository.WishlistSubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class BookServiceIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @Autowired
    private WishlistSubscriptionRepository wishlistSubscriptionRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        exchangeRequestRepository.deleteAll();
        userNotificationRepository.deleteAll();
        wishlistSubscriptionRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createBook_ShouldPersistBookForCurrentUser_WithResolvedImageUrl() {
        Role userRole = ensureRole("ROLE_USER");
        User owner = saveUser("book_owner", "book.owner@example.com", userRole);
        authenticateAs(owner);

        BookRequest request = new BookRequest(
            "Clean Code",
            "Robert Martin",
            "Programming",
            "A classic software craftsmanship book",
            null,
            "9780132350884",
            "Good"
        );

        BookResponse response = bookService.createBook(request);

        assertNotNull(response.getId());
        assertEquals(owner.getId(), response.getOwnerId());
        assertTrue(Boolean.TRUE.equals(response.getAvailable()));
        assertTrue(response.getImageUrl().contains("covers.openlibrary.org"));

        Book savedBook = bookRepository.findById(response.getId()).orElseThrow();
        assertEquals("Clean Code", savedBook.getTitle());
        assertEquals(owner.getId(), savedBook.getOwner().getId());
    }

    @Test
    void markBookAvailability_ShouldUpdateAvailability_ForOwner() {
        Role userRole = ensureRole("ROLE_USER");
        User owner = saveUser("availability_owner", "availability.owner@example.com", userRole);
        Book book = saveBook("Domain-Driven Design", "9780321125217", owner, true);
        authenticateAs(owner);

        BookResponse updated = bookService.markBookAvailability(book.getId(), false);

        assertEquals(false, updated.getAvailable());
        Book persisted = bookRepository.findById(book.getId()).orElseThrow();
        assertEquals(false, persisted.getAvailable());
    }

    private Role ensureRole(String roleName) {
        return roleRepository.findByName(roleName)
            .orElseGet(() -> roleRepository.save(new Role(null, roleName)));
    }

    private User saveUser(String username, String email, Role... roles) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setEmailVerified(true);
        user.setRoles(Set.of(roles));
        return userRepository.save(user);
    }

    private Book saveBook(String title, String isbn, User owner, boolean available) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor("Test Author");
        book.setGenre("Technology");
        book.setLanguage("English");
        book.setIsbn(isbn);
        book.setPublicationYear(2024);
        book.setBookCondition("Good");
        book.setDescription("Integration test book");
        book.setOwner(owner);
        book.setAvailable(available);
        return bookRepository.save(book);
    }

    private void authenticateAs(User user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
            .map(Role::getName)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword(), authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
