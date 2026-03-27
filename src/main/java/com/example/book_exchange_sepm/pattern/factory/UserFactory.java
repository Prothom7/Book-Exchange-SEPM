package com.example.book_exchange_sepm.pattern.factory;

import com.example.book_exchange_sepm.entity.Role;
import com.example.book_exchange_sepm.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

public class UserFactory {

    public enum UserType {
        USER,
        MODERATOR
    }

    public User create(UserType type,
                       String username,
                       String email,
                       String rawPassword,
                       PasswordEncoder passwordEncoder,
                       Role userRole,
                       Role moderatorRole) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmailVerified(true);

        if (type == UserType.MODERATOR) {
            user.setRoles(Set.of(userRole, moderatorRole));
        } else {
            user.setRoles(Set.of(userRole));
        }

        return user;
    }
}
