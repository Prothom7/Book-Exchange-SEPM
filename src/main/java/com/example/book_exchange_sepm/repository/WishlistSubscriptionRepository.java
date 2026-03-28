package com.example.book_exchange_sepm.repository;

import com.example.book_exchange_sepm.entity.WishlistSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistSubscriptionRepository extends JpaRepository<WishlistSubscription, Long> {

    List<WishlistSubscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<WishlistSubscription> findByUserIdAndActiveTrue(Long userId);

    Optional<WishlistSubscription> findByIdAndUserId(Long id, Long userId);

    List<WishlistSubscription> findByActiveTrue();
}
