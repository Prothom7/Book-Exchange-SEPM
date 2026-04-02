package com.example.book_exchange_sepm.config;

import com.example.book_exchange_sepm.entity.*;
import com.example.book_exchange_sepm.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Production Data Seeder for Book Exchange Application
 * 
 * This seeder creates realistic test data for:
 * - 40+ books owned by various users
 * - 10-15 exchange requests with varied statuses
 * - Wishlist subscriptions for multiple users
 * 
 * Only runs if books table is empty (prevents duplicate seeding)
 */
@Configuration
public class ProductionDataSeeder {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Bean
    CommandLineRunner seedProductionData(
            UserRepository userRepository,
            RoleRepository roleRepository,
            BookRepository bookRepository,
            ExchangeRequestRepository exchangeRequestRepository,
            WishlistSubscriptionRepository wishlistSubscriptionRepository) {
        
        return args -> {
            // Only seed if books table is empty
            if (bookRepository.count() > 0) {
                System.out.println("✓ Book data already exists - seeding skipped");
                return;
            }

            System.out.println("\n========================================");
            System.out.println("Starting Production Data Seeding...");
            System.out.println("========================================\n");

            List<User> users = ensureUsers(userRepository, roleRepository);
            System.out.println("✓ Ensured " + users.size() + " users");

            try {
                // Create books
                List<Book> books = createBooks(bookRepository, users);
                System.out.println("✓ Created " + books.size() + " books");

                // Create exchange requests
                List<ExchangeRequest> exchanges = createExchangeRequests(exchangeRequestRepository, bookRepository, users, books);
                System.out.println("✓ Created " + exchanges.size() + " exchange requests");

                // Create wishlist subscriptions
                List<WishlistSubscription> wishlists = createWishlistSubscriptions(wishlistSubscriptionRepository, users);
                System.out.println("✓ Created " + wishlists.size() + " wishlist subscriptions");

                System.out.println("\n========================================");
                System.out.println("Data Seeding Complete!");
                System.out.println("========================================\n");
                System.out.println("Test Accounts:");
                System.out.println("  Admin: admin / admin123");
                System.out.println("  Moderator: john_mod / password123");
                System.out.println("  User: alice_johnson / password123");
                System.out.println("========================================\n");

            } catch (Exception e) {
                System.out.println("✗ Error during seeding: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    /**
     * Ensure a realistic user base exists (10-20 users)
     */
    private List<User> ensureUsers(UserRepository userRepository, RoleRepository roleRepository) {
        String[] roleNames = {"ROLE_ADMIN", "ROLE_DELIVERY_MAN", "ROLE_MODERATOR", "ROLE_USER"};
        Map<String, Role> roles = new HashMap<>();

        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(new Role(null, roleName)));
            roles.put(roleName, role);
        }

        createUserIfMissing(userRepository, "admin", "admin@example.com", "admin123", roles.get("ROLE_ADMIN"));
        createUserIfMissing(userRepository, "david_delivery", "david.delivery@example.com", "password123", roles.get("ROLE_DELIVERY_MAN"));
        createUserIfMissing(userRepository, "nina_delivery", "nina.delivery@example.com", "password123", roles.get("ROLE_DELIVERY_MAN"));
        createUserIfMissing(userRepository, "john_mod", "john.mod@example.com", "password123", roles.get("ROLE_MODERATOR"));
        createUserIfMissing(userRepository, "sarah_mod", "sarah.mod@example.com", "password123", roles.get("ROLE_MODERATOR"));

        String[] names = {
                "alice_johnson", "bob_johnson", "charlie_johnson", "diana_johnson", "eve_johnson",
                "frank_johnson", "grace_johnson", "henry_johnson", "iris_johnson", "jack_johnson",
                "karen_johnson", "leo_johnson", "mia_roberts", "noah_clark", "olivia_wright"
        };

        for (String username : names) {
            createUserIfMissing(userRepository, username, username.replace("_", ".") + "@example.com", "password123", roles.get("ROLE_USER"));
        }

        return userRepository.findAll();
    }

    private void createUserIfMissing(UserRepository userRepository, String username, String email, String rawPassword, Role role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmailVerified(true);
        user.setRoles(new HashSet<>(Collections.singletonList(role)));
        userRepository.save(user);
    }

    /**
     * Create 40-50 books with varied genres and owners
     */
    private List<Book> createBooks(BookRepository bookRepository, List<User> users) {
        List<Book> books = new ArrayList<>();

        String[][] bookData = {
            {"The Great Gatsby", "F. Scott Fitzgerald", "Fiction", "978-0743273565", "English", "1925", "Good", "A classic American novel about the Jazz Age"},
            {"To Kill a Mockingbird", "Harper Lee", "Fiction", "978-0061120084", "English", "1960", "Excellent", "A gripping tale of racial injustice and innocence"},
            {"1984", "George Orwell", "Science Fiction", "978-0451524935", "English", "1949", "Good", "A dystopian novel about totalitarianism"},
            {"Pride and Prejudice", "Jane Austen", "Romance", "978-0141439518", "English", "1813", "Good", "A romantic novel of manners"},
            {"The Catcher in the Rye", "J.D. Salinger", "Fiction", "978-0316769174", "English", "1951", "Fair", "Coming-of-age story of Holden Caulfield"},
            {"Brave New World", "Aldous Huxley", "Science Fiction", "978-0060085239", "English", "1932", "Excellent", "A dystopian vision of the future"},
            {"The Lord of the Rings", "J.R.R. Tolkien", "Fantasy", "978-0544003415", "English", "1954", "Very Good", "Epic fantasy adventure saga"},
            {"Harry Potter", "J.K. Rowling", "Fantasy", "978-0747532699", "English", "1997", "Good", "Magical adventure of a young wizard"},
            {"The Hobbit", "J.R.R. Tolkien", "Fantasy", "978-0547928217", "English", "1937", "Good", "Prequel to The Lord of the Rings"},
            {"Dune", "Frank Herbert", "Science Fiction", "978-0441172719", "English", "1965", "Excellent", "Epic sci-fi about desert planets and politics"},
            {"Foundation", "Isaac Asimov", "Science Fiction", "978-0553293357", "English", "1951", "Very Good", "Asimov's sci-fi masterpiece"},
            {"Neuromancer", "William Gibson", "Science Fiction", "978-0441569595", "English", "1984", "Fair", "Cyberpunk classic"},
            {"A Brief History of Time", "Stephen Hawking", "Science", "978-0553380163", "English", "1988", "Good", "Accessible science about the universe"},
            {"Sapiens", "Yuval Noah Harari", "History", "978-0062316097", "English", "2011", "Excellent", "History of humankind"},
            {"Educated", "Tara Westover", "Biography", "978-0399590504", "English", "2018", "Excellent", "Memoir of education and family"},
            {"The Art of War", "Sun Tzu", "Philosophy", "978-0140019438", "English", "500", "Good", "Ancient Chinese military strategy"},
            {"Meditations", "Marcus Aurelius", "Philosophy", "978-0140449334", "English", "170", "Good", "Stoic philosophy and personal thoughts"},
            {"Crime and Punishment", "Fyodor Dostoevsky", "Fiction", "978-0486425597", "English", "1866", "Fair", "Psychological novel about morality"},
            {"War and Peace", "Leo Tolstoy", "Fiction", "978-0199232765", "English", "1869", "Good", "Massive novel of Russian society"},
            {"The Odyssey", "Homer", "Fiction", "978-0140268867", "English", "800", "Fair", "Ancient epic of Odysseus"},
            {"The Iliad", "Homer", "Fiction", "978-0140275895", "English", "800", "Fair", "Ancient Greek epic of the Trojan Wars"},
            {"Jane Eyre", "Charlotte Bronte", "Romance", "978-0141441146", "English", "1847", "Good", "Gothic romance and self-discovery"},
            {"Wuthering Heights", "Emily Bronte", "Romance", "978-0141439556", "English", "1847", "Fair", "Passionate and dark romance"},
            {"The Picture of Dorian Gray", "Oscar Wilde", "Fiction", "978-0141439570", "English", "1890", "Good", "Tale of vanity and corruption"},
            {"Frankenstein", "Mary Shelley", "Science Fiction", "978-0141439471", "English", "1818", "Fair", "Gothic science fiction classic"},
            {"Dracula", "Bram Stoker", "Horror", "978-0141439846", "English", "1897", "Good", "Gothic vampire horror"},
            {"Ulysses", "James Joyce", "Fiction", "978-0199232765", "English", "1922", "Good", "Modernist epic in Dublin"},
            {"Catch-22", "Joseph Heller", "Fiction", "978-0684801221", "English", "1961", "Excellent", "Satirical war novel"},
            {"One Hundred Years of Solitude", "Gabriel García Márquez", "Fiction", "978-0060883287", "English", "1967", "Very Good", "Magical realism masterpiece"},
            {"A Tale of Two Cities", "Charles Dickens", "Fiction", "978-0141439600", "English", "1859", "Fair", "French Revolution drama"},
            {"Great Expectations", "Charles Dickens", "Fiction", "978-0141439563", "English", "1860", "Good", "Coming-of-age story"},
            {"The Count of Monte Cristo", "Alexandre Dumas", "Adventure", "978-0141442884", "English", "1844", "Excellent", "Adventure and revenge"},
            {"Les Miserables", "Victor Hugo", "Fiction", "978-0451524935", "English", "1862", "Good", "Epic of love and revolution"},
            {"The Brothers Karamazov", "Fyodor Dostoevsky", "Fiction", "978-0374528379", "English", "1879", "Good", "Philosophical novel on faith"},
            {"Anna Karenina", "Leo Tolstoy", "Fiction", "978-0199232765", "English", "1877", "Very Good", "Russian novel of love and society"},
            {"And Then There Were None", "Agatha Christie", "Mystery", "978-0062073556", "English", "1939", "Excellent", "Mystery masterpiece"},
            {"Murder on the Orient Express", "Agatha Christie", "Mystery", "978-0062073549", "English", "1934", "Excellent", "Hercule Poirot mystery"},
            {"The Girl on the Train", "Paula Hawkins", "Mystery", "978-0345549357", "English", "2015", "Excellent", "Contemporary psychological thriller"},
            {"Atomic Habits", "James Clear", "Self-Help", "978-0735211292", "English", "2018", "Excellent", "Guide to building habits"},
            {"Thinking, Fast and Slow", "Daniel Kahneman", "Psychology", "978-0374533557", "English", "2011", "Very Good", "Psychology of decision making"},
            {"The Selfish Gene", "Richard Dawkins", "Science", "978-0192860926", "English", "1976", "Good", "Evolution and genetics"},
        };

        Random random = new Random();
        Set<String> isbnSet = new HashSet<>();

        for (String[] data : bookData) {
            String isbn = data[3];
            
            // Skip if ISBN already used
            if (isbnSet.contains(isbn)) continue;
            isbnSet.add(isbn);

            Book book = new Book();
            book.setTitle(data[0]);
            book.setAuthor(data[1]);
            book.setGenre(data[2]);
            book.setIsbn(isbn);
            book.setLanguage(data[4]);
            book.setPublicationYear(Integer.parseInt(data[5]));
            book.setBookCondition(data[6]);
            book.setDescription(data[7]);
            book.setAvailable(random.nextBoolean());
            
            // Assign random owner from users
            User owner = users.get(random.nextInt(Math.min(users.size(), 15)));
            book.setOwner(owner);
            
            books.add(bookRepository.save(book));
        }

        return books;
    }

    /**
     * Create 10-15 exchange requests with varied statuses
     */
    private List<ExchangeRequest> createExchangeRequests(
            ExchangeRequestRepository exchangeRequestRepository,
            BookRepository bookRepository,
            List<User> users,
            List<Book> books) {

        if (books.size() < 3) {
            return new ArrayList<>();
        }

        List<ExchangeRequest> exchanges = new ArrayList<>();
        Random random = new Random();

        String[] statuses = {"PENDING", "APPROVED", "REJECTED", "CANCELLED"};
        String[] messages = {
            "Great book! I have this one and would like to exchange.",
            "I'm very interested in this title. Here's what I can offer.",
            "This looks like an interesting read. Would you exchange?",
            "I've been looking for this! Can we arrange an exchange?",
            "Perfect match for my collection. Let's trade.",
            "I think we have compatible books. Interested?",
            "This would be a great addition to my library.",
            "Would you be open to this exchange?"
        };

        // Create 10-15 exchange requests
        for (int i = 0; i < Math.min(15, books.size() - 1); i++) {
            ExchangeRequest exchange = new ExchangeRequest();

            // Random requester and book owner
            User requester = users.get(random.nextInt(users.size()));
            User bookOwner = users.get(random.nextInt(users.size()));
            
            // Make sure they're different
            while (bookOwner.getId().equals(requester.getId())) {
                bookOwner = users.get(random.nextInt(users.size()));
            }

            Book requestedBook = books.get(random.nextInt(books.size()));
            requestedBook.setOwner(bookOwner); // Ensure ownership
            bookRepository.save(requestedBook);

            Book offeredBook = books.get(random.nextInt(books.size()));
            offeredBook.setOwner(requester); // Requester owns this book
            bookRepository.save(offeredBook);

            exchange.setRequester(requester);
            exchange.setBook(requestedBook);
            exchange.setOfferedBook(offeredBook);
            exchange.setMessage(messages[random.nextInt(messages.length)]);
            
            // Set status
            String status = statuses[random.nextInt(statuses.length)];
            exchange.setStatus(ExchangeRequest.Status.valueOf(status));

            // If approved/rejected, add reviewer and review date
            if (!status.equals("PENDING")) {
                exchange.setReviewedBy(users.get(0)); // Admin reviews
                exchange.setReviewedAt(LocalDateTime.now().minusDays(random.nextInt(7)));
                if (status.equals("REJECTED")) {
                    exchange.setModeratorComment("Book condition does not match the requirements.");
                }
            }

            exchanges.add(exchangeRequestRepository.save(exchange));
        }

        return exchanges;
    }

    /**
     * Create wishlist subscriptions for users
     */
    private List<WishlistSubscription> createWishlistSubscriptions(
            WishlistSubscriptionRepository wishlistSubscriptionRepository,
            List<User> users) {

        List<WishlistSubscription> wishlists = new ArrayList<>();
        Random random = new Random();

        String[][] wishlistData = {
            {"The Midnight Library", "Matt Haig", "Fiction"},
            {"Project Hail Mary", "Andy Weir", "Science Fiction"},
            {"It Ends with Us", "Colleen Hoover", "Romance"},
            {"The Silent Patient", "Alex Michaelides", "Mystery"},
            {"Verity", "Colleen Hoover", "Thriller"},
            {"Educated", "Tara Westover", "Biography"},
            {"Atomic Habits", "James Clear", "Self-Help"},
            {"Sapiens", "Yuval Noah Harari", "History"},
            {"The Midnight Sky", "Mark Hazeltine", "Science Fiction"},
            {"Piranesi", "Susanna Clarke", "Fantasy"},
            {"The Seven Husbands of Evelyn Hugo", "Taylor Jenkins Reid", "Fiction"},
            {"Circe", "Madeline Miller", "Mythology"},
            {"The Song of Achilles", "Madeline Miller", "Mythology"},
            {"Daisy Jones and The Six", "Taylor Jenkins Reid", "Fiction"},
            {"Four Winds", "Kristin Hannah", "Historical Fiction"},
        };

        // Create 15-20 wishlist subscriptions
        for (int i = 0; i < Math.min(20, wishlistData.length); i++) {
            WishlistSubscription wishlist = new WishlistSubscription();
            User user = users.get(random.nextInt(users.size()));
            String[] data = wishlistData[i % wishlistData.length];

            wishlist.setUser(user);
            wishlist.setBookTitle(data[0]);
            wishlist.setAuthor(data[1]);
            wishlist.setGenre(data[2]);
            wishlist.setActive(true);

            wishlists.add(wishlistSubscriptionRepository.save(wishlist));
        }

        return wishlists;
    }
}
