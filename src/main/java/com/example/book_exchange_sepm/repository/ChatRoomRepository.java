package com.example.book_exchange_sepm.repository;

import com.example.book_exchange_sepm.entity.ChatRoom;
import com.example.book_exchange_sepm.entity.ExchangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    
    /**
     * Find chat room by exchange request
     */
    Optional<ChatRoom> findByExchangeRequest(ExchangeRequest exchangeRequest);
    
    /**
     * Find chat room by exchange request ID
     */
    Optional<ChatRoom> findByExchangeRequest_Id(Long exchangeRequestId);
    
    /**
     * Check if chat room exists for exchange request
     */
    boolean existsByExchangeRequest_Id(Long exchangeRequestId);
}
