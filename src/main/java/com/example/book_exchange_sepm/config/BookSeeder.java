package com.example.book_exchange_sepm.config;

import com.example.book_exchange_sepm.entity.Role;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.repository.RoleRepository;
import com.example.book_exchange_sepm.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashSet;

@Configuration
public class BookSeeder {

    @Bean
    CommandLineRunner seedBooks(UserRepository userRepository, RoleRepository roleRepository) {
        return args -> {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_ADMIN")));
            Role moderatorRole = roleRepository.findByName("ROLE_MODERATOR")
                .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_MODERATOR")));

            ensureUser(userRepository, "admin", "admin@example.com", "admin123", adminRole);
            ensureUser(userRepository, "moderator", "moderator@example.com", "moderator123", moderatorRole);
        };
    }

    private void ensureUser(UserRepository userRepository,
                            String username,
                            String email,
                            String rawPassword,
                            Role role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(new BCryptPasswordEncoder().encode(rawPassword));
        HashSet<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        user.setEmailVerified(true);
        userRepository.save(user);
        System.out.println("Seeded default user: username='" + username + "'");
    }
}
