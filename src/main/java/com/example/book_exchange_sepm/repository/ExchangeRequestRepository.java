package com.example.book_exchange_sepm.repository;

import com.example.book_exchange_sepm.entity.ExchangeRequest;
import com.example.book_exchange_sepm.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, Long> {
    List<ExchangeRequest> findByBookOwner_Id(Long ownerId);
    List<ExchangeRequest> findByRequester_Id(Long requesterId);
    List<ExchangeRequest> findByBook(Book book);
}
