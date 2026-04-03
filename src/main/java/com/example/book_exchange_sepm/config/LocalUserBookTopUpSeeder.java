package com.example.book_exchange_sepm.config;

import com.example.book_exchange_sepm.entity.Book;
import com.example.book_exchange_sepm.entity.Role;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.repository.BookRepository;
import com.example.book_exchange_sepm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class LocalUserBookTopUpSeeder {

    private static final int TARGET_BOOKS_PER_USER = 5;

    private static final String[][] BOOK_TEMPLATES = {
            {"The Midnight Library", "Matt Haig", "Fiction", "English", "2020", "Very Good", "A reflective novel about second chances."},
            {"Project Hail Mary", "Andy Weir", "Science Fiction", "English", "2021", "Excellent", "A fast-paced mission to save humanity."},
            {"The Alchemist", "Paulo Coelho", "Philosophy", "English", "1988", "Good", "A simple story about dreams and purpose."},
            {"The Kite Runner", "Khaled Hosseini", "Fiction", "English", "2003", "Good", "A moving novel about friendship and regret."},
            {"Ikigai", "Hector Garcia", "Self-Help", "English", "2016", "Very Good", "A book about purpose and a balanced life."},
            {"The Silent Patient", "Alex Michaelides", "Mystery", "English", "2019", "Excellent", "A psychological thriller with twists."},
            {"Deep Work", "Cal Newport", "Productivity", "English", "2016", "Good", "Practical advice for focused work."},
            {"The Book Thief", "Markus Zusak", "Historical Fiction", "English", "2005", "Very Good", "A story of books and resilience."},
            {"Rich Dad Poor Dad", "Robert Kiyosaki", "Finance", "English", "1997", "Fair", "A popular personal finance starter."},
            {"Norwegian Wood", "Haruki Murakami", "Fiction", "English", "1987", "Good", "A quiet, emotional coming-of-age novel."}
    };

    @Bean
    @Order(200)
    CommandLineRunner topUpLocalUserBooks(
            UserRepository userRepository,
            BookRepository bookRepository,
            JdbcTemplate jdbcTemplate,
            @Value("${spring.datasource.url:}") String datasourceUrl) {

        return args -> {
            if (!isLocalDevelopmentDatabase(datasourceUrl)) {
                System.out.println("Skipping local user book top-up for non-local database");
                return;
            }

            List<User> users = userRepository.findAll().stream()
                    .filter(this::isNormalUser)
                    .toList();

            int createdCount = 0;
            for (User user : users) {
                int existingBooks = bookRepository.findByOwner(user).size();
                if (existingBooks >= TARGET_BOOKS_PER_USER) {
                    continue;
                }

                int needed = TARGET_BOOKS_PER_USER - existingBooks;
                for (int i = 0; i < needed; i++) {
                    Book book = createTopUpBook(user, existingBooks + i);
                    insertBookCompatibly(jdbcTemplate, book);
                    createdCount++;
                }
            }

            System.out.println("Local user book top-up complete. Added " + createdCount + " books.");
        };
    }

    private boolean isLocalDevelopmentDatabase(String datasourceUrl) {
        String url = datasourceUrl == null ? "" : datasourceUrl.toLowerCase();
        return (url.contains("jdbc:postgresql://postgres:5432/")
                || url.contains("jdbc:postgresql://localhost:5432/"))
                && !url.contains("render.com");
    }

    private boolean isNormalUser(User user) {
        List<String> roleNames = new ArrayList<>();
        for (Role role : user.getRoles()) {
            roleNames.add(role.getName());
        }
        return roleNames.contains("ROLE_USER")
                && !roleNames.contains("ROLE_ADMIN")
                && !roleNames.contains("ROLE_MODERATOR")
                && !roleNames.contains("ROLE_DELIVERY_MAN");
    }

    private Book createTopUpBook(User owner, int index) {
        String[] template = BOOK_TEMPLATES[Math.floorMod(owner.getUsername().hashCode() + index, BOOK_TEMPLATES.length)];

        Book book = new Book();
        book.setTitle(template[0] + " - " + owner.getUsername() + " Copy " + (index + 1));
        book.setAuthor(template[1]);
        book.setGenre(template[2]);
        book.setLanguage(template[3]);
        book.setPublicationYear(Integer.parseInt(template[4]));
        book.setBookCondition(template[5]);
        book.setDescription(template[6]);
        book.setOwner(owner);
        book.setAvailable(true);
        book.setIsbn(buildUniqueIsbn(owner.getUsername(), index));
        return book;
    }

    private String buildUniqueIsbn(String username, int index) {
        int base = Math.abs(username.hashCode());
        return "LOCAL-" + base + "-" + (index + 1);
    }

    private void insertBookCompatibly(JdbcTemplate jdbcTemplate, Book book) {
        boolean hasLegacyBookCondition = hasColumn(jdbcTemplate, "books", "book_condition");
        boolean hasModernCondition = hasColumn(jdbcTemplate, "books", "condition");
        LocalDateTime now = LocalDateTime.now();

        if (hasLegacyBookCondition && hasModernCondition) {
            jdbcTemplate.update(
                    """
                    insert into books
                    (title, author, genre, language, isbn, publication_year, book_condition, condition, description, image_url, owner_id, available, created_at, updated_at)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    book.getTitle(),
                    book.getAuthor(),
                    book.getGenre(),
                    book.getLanguage(),
                    book.getIsbn(),
                    book.getPublicationYear(),
                    book.getBookCondition(),
                    book.getBookCondition(),
                    book.getDescription(),
                    book.getImageUrl(),
                    book.getOwner().getId(),
                    book.getAvailable(),
                    Timestamp.valueOf(now),
                    Timestamp.valueOf(now)
            );
            return;
        }

        if (hasLegacyBookCondition) {
            jdbcTemplate.update(
                    """
                    insert into books
                    (title, author, genre, language, isbn, publication_year, book_condition, description, image_url, owner_id, available, created_at, updated_at)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    book.getTitle(),
                    book.getAuthor(),
                    book.getGenre(),
                    book.getLanguage(),
                    book.getIsbn(),
                    book.getPublicationYear(),
                    book.getBookCondition(),
                    book.getDescription(),
                    book.getImageUrl(),
                    book.getOwner().getId(),
                    book.getAvailable(),
                    Timestamp.valueOf(now),
                    Timestamp.valueOf(now)
            );
            return;
        }

        jdbcTemplate.update(
                """
                insert into books
                (title, author, genre, language, isbn, publication_year, condition, description, image_url, owner_id, available, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                book.getTitle(),
                book.getAuthor(),
                book.getGenre(),
                book.getLanguage(),
                book.getIsbn(),
                book.getPublicationYear(),
                book.getBookCondition(),
                book.getDescription(),
                book.getImageUrl(),
                book.getOwner().getId(),
                book.getAvailable(),
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
        );
    }

    private boolean hasColumn(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.columns
                where table_name = ?
                  and column_name = ?
                """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }
}
