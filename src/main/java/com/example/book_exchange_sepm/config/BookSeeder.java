package com.example.book_exchange_sepm.config;

import com.example.book_exchange_sepm.repository.BookRepository;
import com.example.book_exchange_sepm.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookSeeder {

    @Bean
    CommandLineRunner seedBooks(BookRepository bookRepository, UserRepository userRepository) {
        return args -> {
        };
    }
}
