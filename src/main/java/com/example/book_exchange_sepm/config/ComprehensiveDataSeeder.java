package com.example.book_exchange_sepm.config;

import com.example.book_exchange_sepm.entity.*;
import com.example.book_exchange_sepm.repository.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Comprehensive Data Seeder for Book Exchange Application
 * 
 * DISABLED: This seeder has a database column mapping issue and is currently disabled.
 * The system uses the existing BookSeeder and RoleInitializer instead.
 * 
 * This seeder creates a realistic dataset with:
 * - 15 users (12 regular users + 2 moderators + 1 admin)
 * - 40 books with varied genres
 * - Exchange requests with varied statuses (PENDING, APPROVED, REJECTED, CANCELLED)
 * - Wishlist subscriptions
 */
// @Configuration
public class ComprehensiveDataSeeder {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // @Bean - DISABLED: Database column mapping issue
    //@Bean
    /*
    CommandLineRunner seedComprehensiveData(
            UserRepository userRepository,
            RoleRepository roleRepository,
            BookRepository bookRepository,
            ExchangeRequestRepository exchangeRequestRepository,
            WishlistSubscriptionRepository wishlistSubscriptionRepository) {
        return args -> {
            // Only seed if database is empty
            if (userRepository.count() > 2) {
                System.out.println("Comprehensive data seeding skipped - data already exists");
                return;
            }

            System.out.println("========================================");
            System.out.println("Starting Comprehensive Data Seeding...");
            System.out.println("========================================");

            // Initialize roles
            Map<String, Role> roles = initializeRoles(roleRepository);

            // Create users
            List<User> users = createUsers(userRepository, roles);
            System.out.println("✓ Created " + users.size() + " users");

            // Create books
            List<Book> books = createBooks(bookRepository, users);
            System.out.println("✓ Created " + books.size() + " books");

            // Create exchange requests with varied statuses
            List<ExchangeRequest> exchanges = createExchangeRequests(exchangeRequestRepository, users, books);
            System.out.println("✓ Created " + exchanges.size() + " exchange requests");

            // Create wishlist subscriptions
            List<WishlistSubscription> wishlists = createWishlistSubscriptions(wishlistSubscriptionRepository, users);
            System.out.println("✓ Created " + wishlists.size() + " wishlist subscriptions");

            System.out.println("========================================");
            System.out.println("Data Seeding Complete!");
            System.out.println("========================================");
            System.out.println("\nTest Credentials:");
            System.out.println("  Admin: admin / admin123");
            System.out.println("  Moderator: john_mod / password123");
            System.out.println("  User: alice_johnson / password123");
            System.out.println("========================================");
        };
    }
    */

    /**
     * Factory Pattern: Initialize all roles
     */
    private Map<String, Role> initializeRoles(RoleRepository roleRepository) {
        Map<String, Role> roleMap = new HashMap<>();
        String[] roleNames = {"ROLE_USER", "ROLE_MODERATOR", "ROLE_ADMIN", "ROLE_DELIVERY_MAN"};

        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(null, roleName)));
            roleMap.put(roleName, role);
        }
        return roleMap;
    }

    /**
     * Factory Pattern: Create 15 users with varied roles
     */
    private List<User> createUsers(UserRepository userRepository, Map<String, Role> roles) {
        List<User> users = new ArrayList<>();

        // Admin user
        users.add(createUser("admin", "admin@example.com", "admin123", true, 
            Collections.singleton(roles.get("ROLE_ADMIN")), userRepository));

        // Delivery users
        users.add(createUser("delivery_one", "delivery.one@example.com", "password123", true,
            Collections.singleton(roles.get("ROLE_DELIVERY_MAN")), userRepository));
        users.add(createUser("delivery_two", "delivery.two@example.com", "password123", true,
            Collections.singleton(roles.get("ROLE_DELIVERY_MAN")), userRepository));

        // Moderators
        users.add(createUser("john_mod", "john.mod@example.com", "password123", true,
            Collections.singleton(roles.get("ROLE_MODERATOR")), userRepository));
        users.add(createUser("sarah_mod", "sarah.mod@example.com", "password123", true,
            Collections.singleton(roles.get("ROLE_MODERATOR")), userRepository));

        // Regular users (12 users)
        String[] firstNames = {"Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", 
                               "Grace", "Henry", "Iris", "Jack", "Karen", "Leo"};
        for (String firstName : firstNames) {
            String username = firstName.toLowerCase() + "_johnson";
            String email = firstName.toLowerCase() + ".johnson@example.com";
            users.add(createUser(username, email, "password123", true,
                Collections.singleton(roles.get("ROLE_USER")), userRepository));
        }

        return users;
    }

    /**
     * Helper method to create a user (Factory Pattern)
     */
    private User createUser(String username, String email, String password, boolean emailVerified,
                           Set<Role> userRoles, UserRepository userRepository) {
        if (userRepository.existsByUsername(username)) {
            return userRepository.findByUsername(username).get();
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmailVerified(emailVerified);
        user.setRoles(userRoles);
        return userRepository.save(user);
    }

    /**
     * Factory Pattern: Create 40 books with varied genres and owners
     */
    private List<Book> createBooks(BookRepository bookRepository, List<User> users) {
        List<Book> books = new ArrayList<>();

        String[] genres = {"Fiction", "Science Fiction", "Mystery", "Romance", "History",
                          "Biography", "Science", "Technology", "Art", "Philosophy"};
        
        String[] titles = {
            "The Great Gatsby", "To Kill a Mockingbird", "1984", "Pride and Prejudice",
            "The Catcher in the Rye", "Dune", "Foundation", "Neuromancer", "Snow Crash",
            "The Hobbit", "Lord of the Rings", "Harry Potter and the Philosopher's Stone",
            "A Brief History of Time", "The Selfish Gene", "Sapiens",
            "Thinking, Fast and Slow", "The Art of War", "Principles", "Educated",
            "Born a Crime", "Becoming", "The Immortal Life of Henrietta Lacks",
            "Atomic Habits", "The 7 Habits", "Good to Great", "Start with Why",
            "Freakonomics", "The Tipping Point", "Outliers", "David Copperfield",
            "Jane Eyre", "Wuthering Heights", "The Handmaid's Tale", "Circe",
            "The Seven Husbands of Evelyn Hugo", "Daisy Jones & The Six", "Piranesi",
            "The Midnight Library", "Project Hail Mary", "The Three-Body Problem"
        };

        String[] authors = {
            "F. Scott Fitzgerald", "Harper Lee", "George Orwell", "Jane Austen",
            "J.D. Salinger", "Frank Herbert", "Isaac Asimov", "William Gibson", "Neal Stephenson",
            "J.R.R. Tolkien", "J.R.R. Tolkien", "J.K. Rowling",
            "Stephen Hawking", "Richard Dawkins", "Yuval Noah Harari",
            "Daniel Kahneman", "Sun Tzu", "Ray Dalio", "Tara Westover",
            "Trevor Noah", "Michelle Obama", "Rebecca Skloot",
            "James Clear", "Stephen Covey", "Jim Collins", "Simon Sinek",
            "Steven D. Levitt", "Malcolm Gladwell", "Malcolm Gladwell", "Charles Dickens",
            "Charlotte Brontë", "Emily Brontë", "Margaret Atwood", "Madeline Miller",
            "Taylor Jenkins Reid", "Taylor Jenkins Reid", "Susanna Clarke",
            "Matt Haig", "Andy Weir", "Liu Cixin"
        };

        String[] conditions = {"Like New", "Very Good", "Good", "Fair", "Poor"};

        for (int i = 0; i < 40; i++) {
            Book book = new Book();
            book.setTitle(titles[i % titles.length]);
            book.setAuthor(authors[i % authors.length]);
            book.setGenre(genres[i % genres.length]);
            book.setBookCondition(conditions[i % conditions.length]);
            book.setIsbn("ISBN-" + String.format("%010d", i + 1000));
            book.setLanguage("English");
            book.setPublicationYear(2000 + (i % 24));
            book.setDescription("A compelling " + genres[i % genres.length].toLowerCase() + " novel. " +
                              "This book explores important themes and is a must-read for " +
                              "anyone interested in " + genres[i % genres.length].toLowerCase() + ".");
            book.setImageUrl("https://via.placeholder.com/300x400?text=" + book.getTitle().replaceAll(" ", "+"));
            
            // Assign owner from users list
            User owner = users.get((i + 3) % users.size()); // Skip admin/moderators for most books
            book.setOwner(owner);
            
            // Make 80% of books available
            book.setAvailable(i % 5 != 0);
            
            books.add(bookRepository.save(book));
        }

        return books;
    }

    /**
     * Factory Pattern: Create exchange requests with varied statuses
     */
    private List<ExchangeRequest> createExchangeRequests(ExchangeRequestRepository exchangeRequestRepository,
                                                        List<User> users, List<Book> books) {
        List<ExchangeRequest> exchanges = new ArrayList<>();
        
        // Moderator references
        User moderator = users.stream()
            .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_MODERATOR")))
            .findFirst()
            .orElse(users.get(1));

        // Status distribution for 15 exchange requests
        // 5 PENDING, 5 APPROVED, 3 REJECTED, 2 CANCELLED
        
        // PENDING requests
        for (int i = 0; i < 5; i++) {
            ExchangeRequest request = createExchangeRequest(
                users.get(3 + i), // Requester
                books.get(5 + i), // Requested book
                books.get(10 + i), // Offered book
                "I have a great book to exchange for this one!",
                ExchangeRequest.Status.PENDING,
                null, // Reviewer
                null, // ReviewedAt
                null  // ModeratorComment
            );
            exchanges.add(exchangeRequestRepository.save(request));
        }

        // APPROVED requests
        for (int i = 0; i < 5; i++) {
            ExchangeRequest request = createExchangeRequest(
                users.get(6 + i), // Requester
                books.get(15 + i), // Requested book
                books.get(20 + i), // Offered book
                "Looking forward to this exchange!",
                ExchangeRequest.Status.APPROVED,
                moderator, // Reviewer
                LocalDateTime.now().minusDays(5 - i), // ReviewedAt
                "Approved - books match well in quality"
            );
            // Mark books as unavailable when approved
            request.getBook().setAvailable(false);
            request.getOfferedBook().setAvailable(false);
            exchanges.add(exchangeRequestRepository.save(request));
        }

        // REJECTED requests
        for (int i = 0; i < 3; i++) {
            ExchangeRequest request = createExchangeRequest(
                users.get(9 + i), // Requester
                books.get(25 + i), // Requested book
                books.get(30 + i), // Offered book
                "Would like to swap books",
                ExchangeRequest.Status.REJECTED,
                moderator, // Reviewer
                LocalDateTime.now().minusDays(3 - i), // ReviewedAt
                "Book condition mismatch - offered book is in poor condition"
            );
            exchanges.add(exchangeRequestRepository.save(request));
        }

        // CANCELLED requests
        for (int i = 0; i < 2; i++) {
            ExchangeRequest request = createExchangeRequest(
                users.get(12 + i), // Requester
                books.get(33 + i), // Requested book
                books.get(35 + i), // Offered book
                "Need to cancel this request",
                ExchangeRequest.Status.CANCELLED,
                null, // Reviewer
                null, // ReviewedAt
                null  // ModeratorComment
            );
            exchanges.add(exchangeRequestRepository.save(request));
        }

        return exchanges;
    }

    /**
     * Helper method to create exchange request (Factory Pattern)
     */
    private ExchangeRequest createExchangeRequest(User requester, Book requestedBook, Book offeredBook,
                                                 String message, ExchangeRequest.Status status,
                                                 User reviewer, LocalDateTime reviewedAt, String comment) {
        ExchangeRequest request = new ExchangeRequest();
        request.setRequester(requester);
        request.setBook(requestedBook);
        request.setOfferedBook(offeredBook);
        request.setMessage(message);
        request.setStatus(status);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(reviewedAt);
        request.setModeratorComment(comment);
        return request;
    }

    /**
     * Strategy Pattern concept: Create wishlist subscriptions with varied search criteria
     */
    private List<WishlistSubscription> createWishlistSubscriptions(
            WishlistSubscriptionRepository wishlistSubscriptionRepository, List<User> users) {
        List<WishlistSubscription> wishlists = new ArrayList<>();

        // Create wishlist subscriptions for various users
        String[] bookTitles = {"Dune", "Foundation", "1984", "Neuromancer", "The Hobbit"};
        String[] genres = {"Science Fiction", "Mystery", "Romance", "History", "Biography"};
        String[] authors = {"Frank Herbert", "Isaac Asimov", "George Orwell", "William Gibson", "J.R.R. Tolkien"};

        // Each regular user (excluding admin/moderators) gets 2-3 wishlist subscriptions
        for (int i = 3; i < Math.min(users.size(), 12); i++) {
            User user = users.get(i);
            
            WishlistSubscription sub1 = new WishlistSubscription();
            sub1.setUser(user);
            sub1.setBookTitle(bookTitles[i % bookTitles.length]);
            sub1.setAuthor(authors[i % authors.length]);
            sub1.setGenre(genres[i % genres.length]);
            sub1.setActive(true);
            wishlists.add(wishlistSubscriptionRepository.save(sub1));

            // Add second subscription
            WishlistSubscription sub2 = new WishlistSubscription();
            sub2.setUser(user);
            sub2.setBookTitle(bookTitles[(i + 1) % bookTitles.length]);
            sub2.setGenre(genres[(i + 1) % genres.length]);
            sub2.setActive(true);
            wishlists.add(wishlistSubscriptionRepository.save(sub2));
        }

        return wishlists;
    }
}
