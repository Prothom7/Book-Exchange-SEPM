package com.example.book_exchange_sepm.config;

import com.example.book_exchange_sepm.model.Book;
import com.example.book_exchange_sepm.repository.BookRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class BookSeeder {

    @Bean
    CommandLineRunner seedBooks(BookRepository bookRepository) {
        return args -> {
            if (bookRepository.count() == 0) {
                List<Book> books = List.of(
                        new Book(
                                "Clean Code",
                                "Robert C. Martin",
                                "Software Engineering",
                                "English",
                                "9780132350884",
                                2008,
                                "Good",
                                "A practical handbook of agile software craftsmanship and clean coding principles.",
                                true
                        ),
                        new Book(
                                "The Pragmatic Programmer",
                                "Andrew Hunt, David Thomas",
                                "Software Engineering",
                                "English",
                                "9780135957059",
                                2019,
                                "Like New",
                                "Modern software development practices, mindset, and practical engineering wisdom.",
                                true
                        ),
                        new Book(
                                "Atomic Habits",
                                "James Clear",
                                "Self Help",
                                "English",
                                "9780735211292",
                                2018,
                                "Good",
                                "Behavior change framework based on tiny habits, systems, and identity shifts.",
                                true
                        ),
                        new Book(
                                "Sapiens",
                                "Yuval Noah Harari",
                                "History",
                                "English",
                                "9780062316110",
                                2015,
                                "Fair",
                                "A narrative of human history from early hunter-gatherers to modern societies.",
                                true
                        ),
                        new Book(
                                "Ikigai",
                                "Hector Garcia, Francesc Miralles",
                                "Lifestyle",
                                "English",
                                "9781786330895",
                                2017,
                                "Good",
                                "Explores Japanese perspectives on purpose, longevity, and balanced living.",
                                true
                        ),
                        new Book(
                                "Norwegian Wood",
                                "Haruki Murakami",
                                "Fiction",
                                "Japanese",
                                "9780375704024",
                                2000,
                                "Fair",
                                "A coming-of-age novel about love, memory, and loneliness in Tokyo.",
                                true
                        ),
                        new Book(
                                "The Alchemist",
                                "Paulo Coelho",
                                "Fiction",
                                "Portuguese",
                                "9780062315007",
                                2014,
                                "Good",
                                "A symbolic journey about destiny, personal legend, and resilience.",
                                true
                        ),
                        new Book(
                                "Introduction to Algorithms",
                                "Thomas H. Cormen",
                                "Computer Science",
                                "English",
                                "9780262046305",
                                2022,
                                "Like New",
                                "Comprehensive algorithms reference for analysis, design, and implementation.",
                                true
                        ),
                        new Book(
                                "Pather Panchali",
                                "Bibhutibhushan Bandyopadhyay",
                                "Classic",
                                "Bengali",
                                "9788171678853",
                                2010,
                                "Good",
                                "A Bengali classic portraying rural life, struggle, and emotional depth.",
                                true
                        ),
                        new Book(
                                "Rich Dad Poor Dad",
                                "Robert T. Kiyosaki",
                                "Finance",
                                "English",
                                "9781612681139",
                                2017,
                                "Fair",
                                "A personal finance perspective on assets, liabilities, and financial mindset.",
                                false
                        )
                );
                bookRepository.saveAll(books);
            }
        };
    }
}
