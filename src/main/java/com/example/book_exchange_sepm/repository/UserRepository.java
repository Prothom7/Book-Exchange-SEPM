package com.example.book_exchange_sepm.repository;

import com.example.book_exchange_sepm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    Optional<User> findByVerificationToken(String verificationToken);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
