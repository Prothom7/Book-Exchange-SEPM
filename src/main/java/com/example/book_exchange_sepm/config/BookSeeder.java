package com.example.book_exchange_sepm.config;

import com.example.book_exchange_sepm.entity.Role;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.repository.BookRepository;
import com.example.book_exchange_sepm.repository.UserRepository;
import com.example.book_exchange_sepm.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashSet;
import java.util.Optional;

@Configuration
public class BookSeeder {

    @Bean
    CommandLineRunner seedBooks(BookRepository bookRepository, UserRepository userRepository, RoleRepository roleRepository) {
        return args -> {
            // Seed admin user if not present
            String adminUsername = "admin";
            String adminEmail = "admin@example.com";
            String adminPassword = "admin123";

            if (!userRepository.existsByUsername(adminUsername)) {
                Optional<Role> adminRoleOpt = roleRepository.findByName("ROLE_ADMIN");
                Role adminRole = adminRoleOpt.orElseGet(() -> roleRepository.save(new Role(null, "ROLE_ADMIN")));

                User admin = new User();
                admin.setUsername(adminUsername);
                admin.setEmail(adminEmail);
                admin.setPassword(new BCryptPasswordEncoder().encode(adminPassword));
                HashSet<Role> roles = new HashSet<>();
                roles.add(adminRole);
                admin.setRoles(roles);
                admin.setEmailVerified(true);
                userRepository.save(admin);
                System.out.println("Seeded default admin user: username='admin', password='admin123'");
            }
        };
    }
}
