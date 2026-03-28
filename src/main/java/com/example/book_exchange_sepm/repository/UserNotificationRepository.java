package com.example.book_exchange_sepm.repository;

import com.example.book_exchange_sepm.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserNotification> findByIdAndUserId(Long id, Long userId);
}
