package com.example.book_exchange_sepm.repository;

import com.example.book_exchange_sepm.model.FeedCard;
import com.example.book_exchange_sepm.model.FeedCardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedCardRepository extends JpaRepository<FeedCard, Long> {

    List<FeedCard> findByTypeAndActiveTrueOrderByDisplayOrderAsc(FeedCardType type);
}
